package com.wzb.rest.demo;

import com.wzb.rest.annation.FailBackResponse;
import com.wzb.rest.annation.LogBack;
import com.wzb.rest.annation.NullResponse;
import com.wzb.rest.log.RestClientLog;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;

public class DemoConfigure {

    /**
     * 空响应
     * @param url 请求连接
     * @return 结果
     */
    @NullResponse
    public Object nullResponse(String url) {
        // 做其他事情
        return new Object();
    }

    /**
     * 降级处理
     * @param throwable 异常
     * @return 结果
     */
    @FailBackResponse
    public Object failResponse(Throwable throwable) {
        // 做其他事情
        return new Object();
    }

    /**
     * bean的名称以request开头则为请求
     * @return converter
     */
    @Bean(name = "requestHttpMessageConverter")
    public HttpMessageConverter<?> request() {
        // 个性化的converter
        return null;
    }

    /**
     * bean的名称以response开头则为响应
     * @return converter
     */
    @Bean(name = "responseHttpMessageConverter")
    public HttpMessageConverter<?> response() {
        // 个性化的converter
        return null;
    }

    /**
     * 请求拦截器
     * @return interceptor
     */
    @Bean
    public ClientHttpRequestInterceptor interceptor() {
        // 个性化的interceptor
        return null;
    }

    /**
     * 日志处理
     * @param log log
     */
    @LogBack
    public void logback(RestClientLog log) {

    }
}
