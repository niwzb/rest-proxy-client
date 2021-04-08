package com.wzb.rest.log;

import org.springframework.http.HttpMethod;

/**
 * 客户端日志类
 */
public class RestClientLog {

    private String url;

    private HttpMethod method;

    private Object parameter;

    private Object response;

    private long speedTime;

    private Throwable throwable;

    private boolean multipart;

    public RestClientLog() {

    }

    public RestClientLog(String url,
                         HttpMethod method,
                         Object parameter,
                         Object response,
                         long speedTime,
                         Throwable throwable) {
        this.url = url;
        this.method = method;
        this.parameter = parameter;
        this.response = response;
        this.speedTime = speedTime;
        this.throwable = throwable;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public Object getParameter() {
        return parameter;
    }

    public Object getResponse() {
        return response;
    }

    public long getSpeedTime() {
        return speedTime;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public boolean hasError() {
        return throwable != null;
    }

    public boolean isMultipart() {
        return multipart;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;

        private HttpMethod method;

        private Object parameter;

        private Object response;

        private long speedTime;

        private Throwable throwable;

        private boolean multipart;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder parameter(Object parameter) {
            this.parameter = parameter;
            return this;
        }

        public Builder response(Object response) {
            this.response = response;
            return this;
        }

        public Builder speedTime(long speedTime) {
            this.speedTime = speedTime;
            return this;
        }

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder multipart(boolean multipart) {
            this.multipart = multipart;
            return this;
        }

        public RestClientLog build() {
            RestClientLog log = new RestClientLog(this.url,
                    this.method,
                    this.parameter,
                    this.response,
                    this.speedTime,
                    this.throwable);
            log.multipart = this.multipart;
            return log;
        }
    }

}
