package com.geostar.geostack.git_branch_manager.service;

import com.geostar.geostack.git_branch_manager.pojo.GitProject;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.util.List;

public interface IGitRepositoryService {

    List<GitProject> getAllGitProject();

    /**
     * 克隆或者拉取项目
     *
     * @param gitProject
     * @return
     */
    boolean cloneOrPull(GitProject gitProject) throws IOException, GitAPIException;

    /**
     * 更新项目信息
     *
     * @param gitProject
     */
    boolean updateGitProjectInfo(GitProject gitProject) throws IOException, GitAPIException;

    /**
     * 创建分支
     *
     * @param gitProject
     * @param branchName
     */
    boolean createBranch(GitProject gitProject, String branchName) throws IOException, GitAPIException;

    /**
     * 切换分支
     *
     * @param gitProject
     * @param branchName
     */
    boolean switchBranch(GitProject gitProject, String branchName) throws GitAPIException, IOException;

    /**
     * 推送代码
     *
     * @param gitProject
     * @return
     */
    boolean push(GitProject gitProject, String message) throws IOException, GitAPIException;

    /**
     * 删除当前分支分支
     *
     * @param gitProject
     * @return
     */
    boolean deleteBranch(GitProject gitProject) throws IOException, GitAPIException;

    /**
     * 获取所有项目的分支交集
     * @param projects
     * @return
     */
    List<String> getBranchIntersect(List<GitProject> projects);

    /**
     * 创建标签
     * @param gitProject
     * @param tagName
     * @param tagLog
     * @return
     */
    boolean createTag(GitProject gitProject, String tagName, String tagLog) throws IOException, GitAPIException;
}
