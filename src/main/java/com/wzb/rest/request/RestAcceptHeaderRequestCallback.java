package com.wzb.rest.request;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

/**
 * RestAcceptHeaderRequestCallback
 */
public class RestAcceptHeaderRequestCallback extends RestHttpEntityRequestCallback {

    private Logger logger = LoggerFactory.getLogger(RestAcceptHeaderRequestCallback.class);

    private final HttpEntity<?> requestEntity;

    /**
     * rest http实体请求回调
     *
     * @param responseType      响应类型
     * @param messageConverters 消息转换器
     */
    public RestAcceptHeaderRequestCallback(Type responseType, List<HttpMessageConverter<?>> messageConverters) {
        this(null, responseType, messageConverters);
    }

    /**
     * rest http实体请求回调
     *
     * @param responseType      响应类型
     * @param messageConverters 消息转换器
     */
    public RestAcceptHeaderRequestCallback(@Nullable Object requestBody,
                                           Type responseType,
                                           List<HttpMessageConverter<?>> messageConverters) {
        super(responseType, messageConverters);
        if (requestBody instanceof HttpEntity) {
            this.requestEntity = (HttpEntity<?>) requestBody;
        } else if (requestBody != null) {
            this.requestEntity = new HttpEntity<>(requestBody);
        } else {
            this.requestEntity = HttpEntity.EMPTY;
        }
    }

    @Override
    public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
        super.doWithRequest(httpRequest);
        Object requestBody = this.requestEntity.getBody();
        if (requestBody == null) {
            HttpHeaders httpHeaders = httpRequest.getHeaders();
            HttpHeaders requestHeaders = this.requestEntity.getHeaders();
            if (!requestHeaders.isEmpty()) {
                requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
            }
            if (httpHeaders.getContentLength() < 0) {
                httpHeaders.setContentLength(0L);
            }
        } else {
            Class<?> requestBodyClass = requestBody.getClass();
            Type requestBodyType = (this.requestEntity instanceof RequestEntity
                    ? ((RequestEntity<?>) this.requestEntity).getType()
                    : requestBodyClass);
            HttpHeaders httpHeaders = httpRequest.getHeaders();
            HttpHeaders requestHeaders = this.requestEntity.getHeaders();
            MediaType requestContentType = requestHeaders.getContentType();
            for (HttpMessageConverter<?> messageConverter : getMessageConverters()) {
                if (messageConverter instanceof GenericHttpMessageConverter) {
                    GenericHttpMessageConverter<Object> genericConverter =
                            (GenericHttpMessageConverter<Object>) messageConverter;
                    if (genericConverter.canWrite(requestBodyType, requestBodyClass, requestContentType)) {
                        if (!requestHeaders.isEmpty()) {
                            requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
                        }
                        logBody(requestBody, requestContentType, genericConverter);
                        genericConverter.write(requestBody, requestBodyType, requestContentType, httpRequest);
                        return;
                    }
                } else if (messageConverter.canWrite(requestBodyClass, requestContentType)) {
                    if (!requestHeaders.isEmpty()) {
                        requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
                    }
                    logBody(requestBody, requestContentType, messageConverter);
                    ((HttpMessageConverter<Object>) messageConverter).write(
                            requestBody, requestContentType, httpRequest);
                    return;
                }
            }
            String message = "No HttpMessageConverter for [" + requestBodyClass.getName() + "]";
            if (requestContentType != null) {
                message += " and content type [" + requestContentType + "]";
            }
            throw new RestClientException(message);
        }
    }

    /**
     * 日志
     *
     * @param body      请求体
     * @param mediaType 媒体类型
     * @param converter 转换器
     */
    private void logBody(Object body, @Nullable MediaType mediaType, HttpMessageConverter<?> converter) {
        if (logger.isDebugEnabled()) {
            if (mediaType != null) {
                logger.debug("Writing [" + body + "] as \"" + mediaType + "\"");
            } else {
                String classname = converter.getClass().getName();
                logger.debug("Writing [" + body + "] with " + classname);
            }
        }
    }
}
