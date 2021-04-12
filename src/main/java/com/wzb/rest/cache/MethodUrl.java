package com.wzb.rest.cache;

import org.springframework.http.HttpMethod;

/**
 * 方法url
 */
public class MethodUrl {

    private String url;
    private String[] produces;
    private HttpMethod httpMethod;

    /**
     * MethodUrl
     *
     * @param url        url
     * @param produces   produces
     * @param httpMethod httpMethod
     */
    public MethodUrl(String url, String[] produces, HttpMethod httpMethod) {
        this.url = url;
        this.produces = produces;
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

    /**
     * 获取请求配置
     * @return produces
     */
    public String[] getProduces() {
        return produces;
    }
}
