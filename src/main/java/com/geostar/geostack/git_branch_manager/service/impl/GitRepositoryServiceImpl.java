package com.geostar.geostack.git_branch_manager.service.impl;

import com.geostar.geostack.git_branch_manager.common.BranchTypeEnum;
import com.geostar.geostack.git_branch_manager.config.GitRepositoryConfig;
import com.geostar.geostack.git_branch_manager.pojo.GitProject;
import com.geostar.geostack.git_branch_manager.service.IGitRepositoryService;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("gitRepositoryService")
public class GitRepositoryServiceImpl implements IGitRepositoryService {
    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryServiceImpl.class);
    @Autowired
    private GitRepositoryConfig gitRepositoryConfig;
    private final CredentialsProvider allowHosts;
    /**
     * 默认远程主机
     */
    private static final String ORIGIN = "origin";
    /**
     * 日志分隔符，用于每次对一个项目操作的结束分隔符
     */
    private static final String LOG_SEPARATOR = "---------------------------当前项目处理完毕---------------------------";

    public GitRepositoryServiceImpl() {
        this.allowHosts = new CredentialsProvider() {
            @Override
            public boolean supports(CredentialItem... items) {
                for (CredentialItem item : items) {
                    if ((item instanceof CredentialItem.YesNoType)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
                for (CredentialItem item : items) {
                    if (item instanceof CredentialItem.YesNoType) {
                        ((CredentialItem.YesNoType) item).setValue(true);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean isInteractive() {
                return false;
            }
        };
    }

    @Override
    public List<GitProject> getAllGitProject() {
        return gitRepositoryConfig.getProjects();
    }

    @Override
    public boolean cloneOrPull(GitProject gitProject) throws IOException, GitAPIException {
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName());
        if (!file.exists()) {
            logger.info("克隆仓库开始：{}", gitProject.getRemoteUrl());
            Git git = Git.cloneRepository()
                    .setURI(gitProject.getRemoteUrl())
                    .setDirectory(file)
                    .setCredentialsProvider(allowHosts)
                    .call();
            git.close();
            logger.info("克隆仓库完毕：{}", gitProject.getRemoteUrl());
        } else {
            Git git = Git.open(file);
            BranchTypeEnum branchType = getBranchType(git, gitProject.getCurrBranch());
            if(BranchTypeEnum.LOCAL == branchType){
                logger.info("本地分支不做拉取：{}，分支：{}", gitProject.getRemoteUrl(), git.getRepository().getBranch());
            }else{
                logger.info("拉取仓库开始：{}，分支：{}", gitProject.getRemoteUrl(), git.getRepository().getBranch());
                git.pull().setCredentialsProvider(allowHosts).call();
                logger.info("拉取仓库完毕：{}，分支：{}", gitProject.getRemoteUrl(), git.getRepository().getBranch());
            }
            git.close();
        }
        getAllRemoteBranch(gitProject);
        logger.info(LOG_SEPARATOR);
        return true;
    }

    @Override
    public boolean updateGitProjectInfo(GitProject gitProject) throws IOException, GitAPIException {
        gitProject.setCurrBranch(null);
        gitProject.setLastCommitId(null);
        gitProject.setLastCommitMessage(null);
        gitProject.setLastCommitUser(null);
        gitProject.setLastCommitEmail(null);
        gitProject.setLastCommitDate(null);
        gitProject.getBranchList().clear();
        gitProject.getTagList().clear();
        gitProject.getUntrackedSet().clear();
        gitProject.getModifiedSet().clear();
        gitProject.getMissingSet().clear();
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        if (file.exists()) {
            Git git = Git.open(file);
            /**
             * 获取最后提交信息
             */
            try {
                RevCommit commit = git.log().call().iterator().next();
                gitProject.setLastCommitId(commit.getId().getName());
                gitProject.setLastCommitMessage(commit.getShortMessage());
                gitProject.setLastCommitUser(commit.getAuthorIdent().getName());
                gitProject.setLastCommitEmail(commit.getAuthorIdent().getEmailAddress());
                gitProject.setLastCommitDate(new Date(new Long(commit.getCommitTime()) * 1000));
            } catch (NoHeadException e) {
                logger.warn("当前仓库没有任何提交信息，仓库地址：" + gitProject.getRemoteUrl());
            }
            /**
             * 获取当前多分支
             */
            gitProject.setCurrBranch(git.getRepository().getBranch());

            /**
             * 获取所有远程分支
             */
            getAllRemoteBranch(gitProject);
            /**
             * 获取标签信息
             */
            List<Ref> tagRefs = git.tagList().call();
            for (Ref tagRef : tagRefs) {
                String tagName = tagRef.getName();
                tagName = tagName.substring("refs/tags/".length(), tagName.length());
                gitProject.getTagList().add(tagName);
            }
            /**
             * 获取工作区文件状态
             */
            Status status = git.status().call();
            gitProject.getUntrackedSet().addAll(status.getUntracked());
            gitProject.getModifiedSet().addAll(status.getModified());
            gitProject.getMissingSet().addAll(status.getMissing());
            git.close();
        }
        return true;
    }

    @Override
    public boolean createBranch(GitProject gitProject, String branchName) throws IOException, GitAPIException {
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        if (file.exists()) {
            logger.info("创建分支开始：{}，分支：{}", gitProject.getRemoteUrl(), branchName);
            Git git = Git.open(file);
            git.branchCreate().setName(branchName).call();
            git.checkout().setName(branchName).call();
            git.close();
            logger.info("创建分支完毕：{}，分支：{}", gitProject.getRemoteUrl(), branchName);
            logger.info(LOG_SEPARATOR);
            return true;
        }
        return false;
    }

    @Override
    public boolean switchBranch(GitProject gitProject, String branchName) throws GitAPIException, IOException {
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        if (file.exists()) {
            logger.info("切换分支开始：{}，分支：{}", gitProject.getRemoteUrl(), branchName);
            Git git = Git.open(file);
            BranchTypeEnum branchType = getBranchType(git, branchName);
            if (BranchTypeEnum.REMOTE == branchType) {
                git.fetch().setRemote(ORIGIN).setCheckFetchedObjects(true).setRefSpecs(new RefSpec("refs/heads/" + branchName + ":" + "refs/heads/" + branchName)).call();
            }
            git.checkout().setName(branchName).call();
            git.close();
            logger.info("切换分支完毕：{}，分支：{}", gitProject.getRemoteUrl(), branchName);
            logger.info(LOG_SEPARATOR);
            return true;
        }
        return false;
    }

    /**
     * 获取分支类型，不负责关闭Git对象
     *
     * @param git
     * @param branch 分支名称
     * @return
     */
    private BranchTypeEnum getBranchType(Git git, String branch) {
        boolean isLocalBranch = false;
        boolean isRemoteBranch = false;
        try {
            List<Ref> localRefs = git.branchList().call();
            String localBranchPrefix = "refs/heads/";
            for (Ref ref : localRefs) {
                String refName = ref.getName();
                if (refName.equals(localBranchPrefix + branch)) {
                    isLocalBranch = true;
                    break;
                }
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
            return BranchTypeEnum.ERROR;
        }
        try {
            List<Ref> remoteRefs = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
            String remoteBranchPrefix = "refs/remotes/origin/";
            for (Ref ref : remoteRefs) {
                String refName = ref.getName();
                if (refName.equals(remoteBranchPrefix + branch)) {
                    isRemoteBranch = true;
                    break;
                }
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
            return BranchTypeEnum.ERROR;
        }
        if (isLocalBranch && isRemoteBranch) {
            return BranchTypeEnum.LOCAL_AND_REMOTE;
        }
        if (isLocalBranch) {
            return BranchTypeEnum.LOCAL;
        }
        if (isRemoteBranch) {
            return BranchTypeEnum.REMOTE;
        }
        return BranchTypeEnum.NOT_EXIST;
    }

    @Override
    public boolean push(GitProject gitProject, String message) throws IOException, GitAPIException {
        logger.info("代码推送开始：{}，message：{}", gitProject.getRemoteUrl(), message);
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        Git git = Git.open(file);
        Status status = git.status().call();
        /**
         * 如果是本地分支或者有修改则提交代码
         */
        if (status.hasUncommittedChanges()) {
            git.add().addFilepattern(".").call();
            git.commit().setAll(true).setMessage(message).call();
        }
        git.push().setPushAll().setCredentialsProvider(allowHosts).call();
        git.close();
        logger.info("代码推送完毕：{}", gitProject.getRemoteUrl());
        logger.info(LOG_SEPARATOR);
        return true;
    }

    @Override
    public boolean deleteBranch(GitProject gitProject) throws IOException, GitAPIException {
        logger.info("删除分支开始：{}", gitProject.getRemoteUrl());
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        Git git = Git.open(file);
        git.checkout().setName("master").call();
        git.branchDelete().setBranchNames(gitProject.getCurrBranch()).setForce(true).call();
        List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        String remoteBranchPrefix = "refs/remotes/origin/";
        for (Ref ref : refs) {
            String refName = ref.getName();
            if (refName.equals(remoteBranchPrefix + gitProject.getCurrBranch())) {
                RefSpec refSpec = new RefSpec()
                        .setSource(null)
                        .setDestination("refs/heads/" + gitProject.getCurrBranch());
                git.push().setRefSpecs(refSpec).setRemote(ORIGIN).call();
                break;
            }
        }
        git.close();
        logger.info("删除分支完毕：{}", gitProject.getRemoteUrl());
        logger.info(LOG_SEPARATOR);
        return true;
    }

    @Override
    public List<String> getBranchIntersect(List<GitProject> projects) {
        List<String> list = new ArrayList<>();
        if (projects.size() > 0) {
            list = projects.get(0).getBranchList();
            for (GitProject gitProject : projects) {
                list.retainAll(gitProject.getBranchList());
            }
        }
        return list;
    }

    @Override
    public boolean createTag(GitProject gitProject, String tagName, String tagLog) throws IOException, GitAPIException {
        logger.info("创建标签开始：{}，标签：{}", gitProject.getRemoteUrl(), tagLog);
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        Git git = Git.open(file);
        git.tag().setName(tagName).setMessage(tagLog).call();
        git.push().setPushTags().call();
        git.close();
        logger.info("创建标签完毕：{}，标签：{}", gitProject.getRemoteUrl(), tagLog);
        logger.info(LOG_SEPARATOR);
        return false;
    }

    @Override
    public List<String> getTagIntersect(List<GitProject> projects) {
        List<String> list = new ArrayList<>();
        if (projects.size() > 0) {
            list = projects.get(0).getTagList();
            for (GitProject gitProject : projects) {
                list.retainAll(gitProject.getTagList());
            }
        }
        return list;
    }

    @Override
    public String getFileContent(GitProject gitProject, String fileName) throws IOException {
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + fileName);
        String content = FileUtils.readFileToString(file, "UTF-8");
        return content;
    }

    /**
     * 获取所有远程分支
     *
     * @param gitProject
     * @return
     * @throws GitAPIException
     */
    public void getAllRemoteBranch(GitProject gitProject) throws GitAPIException, IOException {
        String workHome = gitRepositoryConfig.getWorkHome();
        /**
         * 获取远程分支信息
         */
        gitProject.getBranchList().clear();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        if (file.exists()) {
            Git git = Git.open(file);
            List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
            for (Ref ref : refs) {
                String branchName = ref.getName();
                /**
                 * 本地分支前缀
                 */
                String headsBranchPrefix = "refs/heads/";
                /**
                 * 远程分支前缀
                 */
                String remoteBranchPrefix = "refs/remotes/origin/";
                if (branchName.startsWith(headsBranchPrefix)) {
                    branchName = branchName.substring(headsBranchPrefix.length(), branchName.length());
                } else if (branchName.startsWith(remoteBranchPrefix)) {
                    branchName = branchName.substring(remoteBranchPrefix.length(), branchName.length());
                }
                if (!gitProject.getBranchList().contains(branchName)) {
                    gitProject.getBranchList().add(branchName);
                }
            }
            git.close();
        }
        List<String> branchList = gitProject.getBranchList();
        List<String> newBranchList = new ArrayList<>();
        if (branchList.contains("master")) {
            newBranchList.add("master");
            branchList.remove(branchList.indexOf("master"));
        }
        if (branchList.contains("develop")) {
            newBranchList.add("develop");
            branchList.remove(branchList.indexOf("develop"));
        }
        for (String branch : branchList) {
            newBranchList.add(branch);
        }
        gitProject.getBranchList().clear();
        gitProject.getBranchList().addAll(newBranchList);
    }

}
