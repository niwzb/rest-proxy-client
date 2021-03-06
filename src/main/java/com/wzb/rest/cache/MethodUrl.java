package com.wzb.rest.cache;

import org.springframework.http.HttpMethod;

/**
 * 方法url
 */
public class MethodUrl {

    private String url;
    private HttpMethod httpMethod;

    /**
     * MethodUrl
     *
     * @param url        url
     * @param httpMethod httpMethod
     */
    public MethodUrl(String url, HttpMethod httpMethod) {
        this.url = url;
        this.httpMethod = httpMethod;
    }

    /**
     * 获取url
     *
     * @return {@link String}
     */
    public String getUrl() {
        return url;
    }

    /**
     * 获取http方法
     *
     * @return {@link HttpMethod}
     */
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }
}
