package com.kyle;

import lombok.Data;

//数据路由配置


public class DBRouterConfig {
    /**
     * 分库数量
     */
    private int dbCount;

    /**
     * 分表数量
     */
    private int tbCount;

    /**
     * 路由字段
     */
    private String routerKey;

    public DBRouterConfig() {
    }

    public DBRouterConfig(int dbCount, int tbCount, String routerKey) {
        this.dbCount = dbCount;
        this.tbCount = tbCount;
        this.routerKey = routerKey;
    }

    /**
     * 获取
     * @return dbCount
     */
    public int getDbCount() {
        return dbCount;
    }

    /**
     * 设置
     * @param dbCount
     */
    public void setDbCount(int dbCount) {
        this.dbCount = dbCount;
    }

    /**
     * 获取
     * @return tbCount
     */
    public int getTbCount() {
        return tbCount;
    }

    /**
     * 设置
     * @param tbCount
     */
    public void setTbCount(int tbCount) {
        this.tbCount = tbCount;
    }

    /**
     * 获取
     * @return routerKey
     */
    public String getRouterKey() {
        return routerKey;
    }

    /**
     * 设置
     * @param routerKey
     */
    public void setRouterKey(String routerKey) {
        this.routerKey = routerKey;
    }

    public String toString() {
        return "DBRouterConfig{dbCount = " + dbCount + ", tbCount = " + tbCount + ", routerKey = " + routerKey + "}";
    }
}
