package com.geostar.geostack.git_branch_manager.service.impl;

import com.geostar.geostack.git_branch_manager.common.BranchTypeEnum;
import com.geostar.geostack.git_branch_manager.common.Page;
import com.geostar.geostack.git_branch_manager.config.GitRepositoryConfig;
import com.geostar.geostack.git_branch_manager.pojo.GitLog;
import com.geostar.geostack.git_branch_manager.pojo.GitProject;
import com.geostar.geostack.git_branch_manager.service.IGitRepositoryService;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service("gitRepositoryService")
public class GitRepositoryServiceImpl implements IGitRepositoryService {
    private static final Logger logger = LoggerFactory.getLogger(GitRepositoryServiceImpl.class);
    @Autowired
    private GitRepositoryConfig gitRepositoryConfig;
    private CredentialsProvider allowHosts;
    /**
     * 默认远程主机
     */
    private static final String ORIGIN = "origin";
    /**
     * 日志分隔符，用于每次对一个项目操作的结束分隔符
     */
    private static final String LOG_SEPARATOR = "---------------------------当前项目处理完毕---------------------------";

    @PostConstruct
    public void initAllowHosts() {
        this.allowHosts = new UsernamePasswordCredentialsProvider(gitRepositoryConfig.getGitUsername(), gitRepositoryConfig.getGitPassword());
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
            if (BranchTypeEnum.LOCAL == branchType) {
                logger.info("本地分支不做拉取：{}，分支：{}", gitProject.getRemoteUrl(), git.getRepository().getBranch());
            } else {
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
        gitProject.getConflictingSet().clear();
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
            gitProject.getConflictingSet().addAll(status.getConflicting());
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
            git.checkout().setCreateBranch(true).setName(branchName).call();
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
                git.fetch().setRemote(ORIGIN).setCheckFetchedObjects(true).setRefSpecs(new RefSpec("refs/heads/" + branchName + ":" + "refs/heads/" + branchName)).setCredentialsProvider(allowHosts).call();
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
        logger.info("删除分支开始：{}，分支：{}", gitProject.getRemoteUrl(), gitProject.getCurrBranch());
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
                git.push().setRefSpecs(refSpec).setRemote(ORIGIN).setCredentialsProvider(allowHosts).call();
                break;
            }
        }
        git.close();
        logger.info("删除分支完毕：{}，分支：{}", gitProject.getRemoteUrl(), gitProject.getCurrBranch());
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
        git.push().setPushTags().setCredentialsProvider(allowHosts).call();
        git.close();
        logger.info("创建标签完毕：{}，标签：{}", gitProject.getRemoteUrl(), tagLog);
        logger.info(LOG_SEPARATOR);
        return false;
    }

    @Override
    public void createBranchByTag(GitProject gitProject, String tagName, String branchName) throws IOException, GitAPIException {
        updateGitProjectInfo(gitProject);
        logger.info("从{}标签检出代码到{}分支，project：{}", tagName, branchName, gitProject.getRemoteUrl());
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        Git git = Git.open(file);
        List<Ref> tagRefs = git.tagList().call();
        for (Ref tagRef : tagRefs) {
            String currTagName = tagRef.getName();
            currTagName = currTagName.substring("refs/tags/".length(), currTagName.length());
            if (currTagName.equals(tagName)) {
                Repository repository = git.getRepository();
                String commitId = repository.peel(tagRef).getPeeledObjectId().getName();
                git.checkout().setCreateBranch(true).setStartPoint(commitId).setName(branchName).call();
                repository.close();
                break;
            }
        }
        git.close();
        logger.info(LOG_SEPARATOR);
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
     * 删除标签，先删除本地标签，再删除远程标签
     *
     * @param gitProject
     * @param tagName
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @Override
    public boolean deleteTag(GitProject gitProject, String tagName) throws IOException, GitAPIException {
        logger.info("删除标签开始：{}，标签：{}", gitProject.getRemoteUrl(), tagName);
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        Git git = Git.open(file);
        List<Ref> refs = git.tagList().call();
        git.tagDelete().setTags(tagName).call();
        String tagPrefix = "refs/tags/";
        for (Ref ref : refs) {
            String refName = ref.getName();
            if (refName.equals(tagPrefix + tagName)) {
                RefSpec refSpec = new RefSpec()
                        .setSource(null)
                        .setDestination(refName);
                git.push().setRefSpecs(refSpec).setRemote(ORIGIN).setCredentialsProvider(allowHosts).call();
                break;
            }
        }
        git.close();
        logger.info("删除标签完毕：{}，标签：{}", gitProject.getRemoteUrl(), tagName);
        logger.info(LOG_SEPARATOR);
        return true;
    }

    /**
     * 合并分支，将被合并分支的修改并入当前工作分支，不使用快进模式
     *
     * @param gitProject
     * @param currWorkBranch 当前工作分支
     * @param sourceBranch   被合并的分支
     * @param message
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    @Override
    public boolean mergeBranch(GitProject gitProject, String currWorkBranch, String sourceBranch, String message) throws IOException, GitAPIException {
        logger.info("合并分支开始：{}，工作分支：{}，被合并分支{}", gitProject.getRemoteUrl(), currWorkBranch, sourceBranch);
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        Git git = Git.open(file);
        Repository repo = git.getRepository();
        if (!repo.getBranch().equals(currWorkBranch)) {
            git.checkout().setName(currWorkBranch).call();
        }
        ObjectId mergeBase = repo.resolve(sourceBranch);
        git.merge().
                include(mergeBase).
                setCommit(true).
                setFastForward(MergeCommand.FastForwardMode.NO_FF).
                setMessage(message).
                call();
        git.push().setPushAll().setCredentialsProvider(allowHosts).setCredentialsProvider(allowHosts).call();
        git.close();
        logger.info("合并分支完成：{}，工作分支：{}，被合并分支{}", gitProject.getRemoteUrl(), currWorkBranch, sourceBranch);
        logger.info(LOG_SEPARATOR);
        return true;
    }

    @Override
    public void getCommitLogs(Page<GitLog> page, String username, String projectName) throws IOException, GitAPIException {
        List<GitLog> logs = page.getAllData();
        List<GitProject> gitProjects = this.getAllGitProject();
        String workHome = gitRepositoryConfig.getWorkHome();
        for (GitProject gitProject : gitProjects) {
            File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
            Git git = Git.open(file);
            Iterable<RevCommit> it = git.log().call();
            for (RevCommit commit : it) {
                GitLog log = new GitLog();
                log.setProjectName(gitProject.getName());
                log.setMessage(commit.getShortMessage());
                log.setUsername(commit.getAuthorIdent().getName());
                log.setCommitId(commit.getId().getName());
                log.setCommitTime(new Date(new Long(commit.getCommitTime()) * 1000));
                if (username == null && projectName == null) {
                    logs.add(log);
                } else if (username != null && projectName != null) {
                    if (log.getUsername().equals(username) && log.getProjectName().equals(projectName)) {
                        logs.add(log);
                    }
                } else if (username != null) {
                    if (log.getUsername().equals(username)) {
                        logs.add(log);
                    }
                } else if (projectName != null) {
                    if (log.getProjectName().equals(projectName)) {
                        logs.add(log);
                    }
                }
            }
        }
        Collections.sort(logs, (arg0, arg1) -> {
            long time0 = arg0.getCommitTime().getTime();
            long time1 = arg1.getCommitTime().getTime();
            if (time1 > time0) {
                return 1;
            } else if (time1 == time0) {
                return 0;
            } else {
                return -1;
            }
        });
        int start = page.getPageIndex() * page.getPageSize();
        int end = ((page.getPageIndex() + 1) * page.getPageSize()) - 1;
        for (int i = start; i <= end; i++) {
            page.getData().add(page.getAllData().get(i));
        }
        page.setTotalDataNum(page.getAllData().size());
        int totalPageNum;
        if (page.getTotalDataNum() % page.getPageSize() == 0) {
            totalPageNum = page.getTotalDataNum() / page.getPageSize();
        } else {
            totalPageNum = page.getTotalDataNum() / page.getPageSize() + 1;
        }
        page.setTotalPageNum(totalPageNum);
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
