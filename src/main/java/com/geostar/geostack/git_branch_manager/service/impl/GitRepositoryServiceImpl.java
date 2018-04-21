package com.geostar.geostack.git_branch_manager.service.impl;

import com.geostar.geostack.git_branch_manager.common.BranchTypeEnum;
import com.geostar.geostack.git_branch_manager.config.GitRepositoryConfig;
import com.geostar.geostack.git_branch_manager.pojo.GitProject;
import com.geostar.geostack.git_branch_manager.service.IGitRepositoryService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
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
            logger.info("克隆仓库：{}", gitProject.getRemoteUrl());
            Git git = Git.cloneRepository()
                    .setURI(gitProject.getRemoteUrl())
                    .setDirectory(file)
                    .setCredentialsProvider(allowHosts)
                    .call();
            git.close();
        } else {
            Git git = Git.open(file);
            logger.info("拉取仓库：{}，分支：{}", gitProject.getRemoteUrl(), git.getRepository().getBranch());
            git.pull().setCredentialsProvider(allowHosts).call();
            git.close();
        }
        getAllRemoteBranch(gitProject);
        return true;
    }

    @Override
    public boolean updateGitProjectInfo(GitProject gitProject) throws IOException, GitAPIException {
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        if (file.exists()) {
            Git git = Git.open(file);
            /**
             * 获取最后提交信息
             */
            RevCommit commit = git.log().call().iterator().next();
            gitProject.setLastCommitId(commit.getId().getName());
            gitProject.setLastCommitUser(commit.getAuthorIdent().getEmailAddress());
            gitProject.setLastCommitDate(new Date(new Long(commit.getCommitTime()) * 1000));
            /**
             * 获取分支信息
             */
            gitProject.setCurrBranch(git.getRepository().getBranch());

            /**
             * 获取所有远程分支
             */
            getAllRemoteBranch(gitProject);
            git.close();
        } else {
            gitProject.setCurrBranch(null);
            gitProject.setLastCommitId(null);
            gitProject.setLastCommitUser(null);
            gitProject.setLastCommitDate(null);
            gitProject.getBranchList().clear();
        }
        return true;
    }

    @Override
    public boolean createBranch(GitProject gitProject, String branchName) throws IOException, GitAPIException {
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        if (file.exists()) {
            Git git = Git.open(file);
            git.branchCreate().setName(branchName).call();
            git.checkout().setName(branchName).call();
            git.close();
            return true;
        }
        return false;
    }

    @Override
    public boolean switchBranch(GitProject gitProject, String branchName) throws GitAPIException, IOException {
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        if (file.exists()) {
            Git git = Git.open(file);
            BranchTypeEnum branchType = getBranchType(git, branchName);
            if (BranchTypeEnum.REMOTE == branchType) {
                git.fetch().setRemote(ORIGIN).setCheckFetchedObjects(true).setRefSpecs(new RefSpec("refs/heads/" + branchName + ":" + "refs/heads/" + branchName)).call();
            }
            git.checkout().setName(branchName).call();
            git.close();
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
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        Git git = Git.open(file);
        git.add().addFilepattern(".").call();
        git.commit().setAll(true).setMessage(message).call();
        git.push().setPushTags().setCredentialsProvider(allowHosts).call();
        git.close();
        return false;
    }

    @Override
    public boolean deleteBranch(GitProject gitProject) throws IOException, GitAPIException {
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
        String workHome = gitRepositoryConfig.getWorkHome();
        File file = new File(workHome + File.separator + gitProject.getName() + File.separator + ".git");
        Git git = Git.open(file);
        git.tag().setName(tagName).setMessage(tagLog).call();
        git.close();
        return false;
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
