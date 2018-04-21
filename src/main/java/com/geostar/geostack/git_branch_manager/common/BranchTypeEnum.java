package com.geostar.geostack.git_branch_manager.common;

/**
 * 分支类型枚举
 * Created by Nihaorz on 2018/4/21.
 */
public enum BranchTypeEnum {

    LOCAL, // 本地分支
    REMOTE, // 远程分支
    LOCAL_AND_REMOTE, // 本地和远程都有
    NOT_EXIST, // 不存在
    ERROR // 因为API或者文件不存在导致获取分支类型出错

}
