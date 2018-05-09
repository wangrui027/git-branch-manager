package com.geostar.geostack.git_branch_manager.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Page分页对象
 *
 * @param <T>
 */
public class Page<T> {

    /**
     * 默认分页大小
     */
    private int pageSize = 10;

    /**
     * 当前页码
     */
    private int pageIndex;

    /**
     * 当前数据总数
     */
    private int totalDataNum;

    /**
     * 当前总页数
     */
    private int totalPageNum;

    private List<T> data = new ArrayList<>();

    private List<T> allData = new ArrayList<>();

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getTotalDataNum() {
        return totalDataNum;
    }

    public void setTotalDataNum(int totalDataNum) {
        this.totalDataNum = totalDataNum;
    }

    public int getTotalPageNum() {
        return totalPageNum;
    }

    public void setTotalPageNum(int totalPageNum) {
        this.totalPageNum = totalPageNum;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public List<T> getAllData() {
        return allData;
    }

    public void setAllData(List<T> allData) {
        this.allData = allData;
    }
}
