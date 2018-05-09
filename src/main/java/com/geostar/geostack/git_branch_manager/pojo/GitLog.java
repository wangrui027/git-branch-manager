package com.geostar.geostack.git_branch_manager.pojo;

import java.util.Date;

/**
 * Git日志对象
 */
public class GitLog {

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 用户名
     */
    private String username;

    /**
     * commitid
     */
    private String commitId;

    /**
     * 日志信息
     */
    private String message;

    /**
     * 提交时间
     */
    private Date commitTime;

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }
}
