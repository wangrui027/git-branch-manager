package com.geostar.geostack.git_branch_manager.pojo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GitProject {

    /**
     * 项目名
     */
    private String name;

    /**
     * 远程地址
     */
    private String remoteUrl;

    /**
     * 最后提交id
     */
    private String lastCommitId;

    /**
     * 最后提交日志
     */
    private String lastCommitMessage;

    /**
     * 最后提交人
     */
    private String lastCommitUser;

    /**
     * 最后提交邮箱
     */
    private String lastCommitEmail;

    /**
     * 最后提交时间
     */
    private Date lastCommitDate;

    /**
     * 当前分支
     */
    private String currBranch;

    /**
     * 分支集合
     */
    private final List<String> branchList = new ArrayList<>();

    /**
     * 标签集合
     */
    private final List<String> tagList = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public String getLastCommitId() {
        return lastCommitId;
    }

    public void setLastCommitId(String lastCommitId) {
        this.lastCommitId = lastCommitId;
    }

    public String getLastCommitMessage() {
        return lastCommitMessage;
    }

    public void setLastCommitMessage(String lastCommitMessage) {
        this.lastCommitMessage = lastCommitMessage;
    }

    public String getLastCommitUser() {
        return lastCommitUser;
    }

    public void setLastCommitUser(String lastCommitUser) {
        this.lastCommitUser = lastCommitUser;
    }

    public String getLastCommitEmail() {
        return lastCommitEmail;
    }

    public void setLastCommitEmail(String lastCommitEmail) {
        this.lastCommitEmail = lastCommitEmail;
    }

    public String getCurrBranch() {
        return currBranch;
    }

    public void setCurrBranch(String currBranch) {
        this.currBranch = currBranch;
    }

    public Date getLastCommitDate() {
        return lastCommitDate;
    }

    public void setLastCommitDate(Date lastCommitDate) {
        this.lastCommitDate = lastCommitDate;
    }

    public List<String> getBranchList() {
        return branchList;
    }

    public List<String> getTagList() {
        return tagList;
    }

}
