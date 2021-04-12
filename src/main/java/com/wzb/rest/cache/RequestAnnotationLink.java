package com.wzb.rest.cache;

import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 请求注解链接
 * @param <T> 请求注解
 * @param <U> 注解类型
 */
public class RequestAnnotationLink<T, U extends Annotation> {

    private RequestAnnotation<U> requestAnnotation;

    private RequestAnnotationLink<T, Annotation> next;

    /**
     * 解析请求注解
     *
     * @param method    方法
     * @param prefixUrl 前缀地址
     * @return          请求URL
     */
    public MethodUrl resolverRequestAnnotation(Method method, String prefixUrl) {
        U annotation = method.getAnnotation(requestAnnotation.getAnnotationClass());
        if (Objects.nonNull(annotation)) {
            String[] value = requestAnnotation.getValueFunction().apply(annotation);
            if (value.length == 0) {
                value = requestAnnotation.getPathFunction().apply(annotation);
            }
            String mapping = value.length > 0 ? value[0] : "";
            HttpMethod httpMethod = requestAnnotation.getDefaultHttpMethod();
            if (Objects.nonNull(requestAnnotation.getHttpMethodFunction())) {
                RequestMethod[] requestMethods = requestAnnotation.getHttpMethodFunction().apply(annotation);
                if (requestMethods.length > 0) {
                    httpMethod = HttpMethod.resolve(requestMethods[0].name());
                }
            }
            String[] produces = requestAnnotation.getProducesFunction().apply(annotation);
            return new MethodUrl(append(prefixUrl , mapping), produces, httpMethod);
        }
        return Objects.nonNull(next) ? next.resolverRequestAnnotation(method, prefixUrl) : null;
    }

    /**
     * 建造
     *
     * @param requestAnnotations 请求注解
     * @return {@link RequestAnnotationLink}
     */
    public static RequestAnnotationLink build(RequestAnnotation... requestAnnotations) {
        RequestAnnotationLink head = new RequestAnnotationLink<>();
        head.requestAnnotation = requestAnnotations[0];
        if (requestAnnotations.length > 1) {
            head.next = new RequestAnnotationLink<>();
        }
        RequestAnnotationLink next = head.next;
        for (int i = 1; i < requestAnnotations.length; i++) {
            next.requestAnnotation = requestAnnotations[i];
            if (i < requestAnnotations.length - 1) {
                next.next = new RequestAnnotationLink<>();
                next = next.next;
            }
        }
        return head;
    }

    /**
     * 拼接URL
     *
     * @param prefixUrl 前缀
     * @param mapping   请求类型注解值
     * @return {@link String}
     */
    private String append(String prefixUrl, String mapping) {
        if (null == mapping || mapping.isEmpty()) {
            return prefixUrl;
        }
        mapping = mapping.replace("\\", "/");
        StringBuilder builder = new StringBuilder(prefixUrl);
        if (builder.charAt(builder.length() - 1) != '/') {
            builder.append("/");
        }
        if (mapping.startsWith("/")) {
            mapping = mapping.substring(1);
        }
        return builder.append(mapping).toString();
    }
}
