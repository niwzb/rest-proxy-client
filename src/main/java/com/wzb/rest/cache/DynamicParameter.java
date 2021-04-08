package com.wzb.rest.cache;

/**
 * 动态参数
 */
public class DynamicParameter {

    private String name;

    private StringBuilder subURL;

    private int endIndex;

    /**
     * 动态参数
     *
     * @param subURL 子段URL
     */
    public DynamicParameter(StringBuilder subURL) {
        this.subURL = subURL;
    }

    /**
     * 动态参数
     *
     * @param name   名称
     * @param subURL 子段URL
     */
    public DynamicParameter(String name, StringBuilder subURL, int endIndex) {
        this.name = name;
        this.subURL = subURL;
        this.endIndex = endIndex;
    }

    public String getName() {
        return name;
    }

    public StringBuilder getSubURL() {
        return subURL;
    }

    public int getEndIndex() {
        return endIndex;
    }
}
