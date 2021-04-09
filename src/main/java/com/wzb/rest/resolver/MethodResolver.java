package com.wzb.rest.resolver;

import com.wzb.rest.cache.ClientCacheFactory;
import com.wzb.rest.cache.MethodUrl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * 方法解析
 */
public final class MethodResolver {

    private static final MethodResolver resolver = new MethodResolver();

    private ClientCacheFactory factory;

    private URLResolver urlResolver;

    private ResponseTypeResolver responseTypeResolver;

    private ParameterResolver parameterResolver;

    private DynamicParameterResolver dynamicParameterResolver;

    /**
     * init
     */
    private MethodResolver() {
        init();
    }

    /**
     * 获取实例
     *
     * @return {@link MethodResolver}
     */
    public static MethodResolver getInstance() {
        return resolver;
    }

    /**
     * 方法解析
     *
     * @param restClientInterface rest客户端接口
     * @param prefixUrl           URL前缀
     */
    public void resolverMethod(Class<?> restClientInterface, String prefixUrl) {
        Method[] methods = restClientInterface.getMethods();
        for(Method method : methods) {
            //方法key
            String methodKey = factory.generateMethodKey(method);
            //解析URL
            MethodUrl methodUrl = urlResolver.resolverUrl(factory, methodKey, method, prefixUrl);
            //请求连接包含静态参数
            factory.putIfAbsent(methodKey, methodUrl.getUrl().contains("?"));
            //方法参数注解，1维是参数，2维是注解
            Annotation[][] annotations = method.getParameterAnnotations();
            //解析方法参数@PathVariable、@RequestParam、@RequestHeader、@RequestBody、@RestRequestBody、@RestRequestFile注解参数
            parameterResolver.resolverParameter(factory, methodKey, annotations, method.getParameters());
            //解析动态参数URL
            dynamicParameterResolver.resolverDynamicParameter(factory, methodKey, methodUrl.getUrl());
            //解析返回类型
            responseTypeResolver.resolverResponseType(factory, methodKey, method.getGenericReturnType());
            //接口方法参数分类
            parameterResolver.resolverParameterType(factory, methodKey);
        }
    }

    /**
     * init
     */
    private void init() {
        factory = ClientCacheFactory.getInstance();

        urlResolver = URLResolver.getInstance();

        responseTypeResolver = ResponseTypeResolver.getInstance();

        parameterResolver = ParameterResolver.getInstance();

        dynamicParameterResolver = DynamicParameterResolver.getInstance();
    }
}
