package com.wzb.rest.resolver;

import com.wzb.rest.cache.ClientCacheFactory;
import com.wzb.rest.cache.MethodUrl;
import org.springframework.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * url解析
 */
public final class URLResolver {

    private static final URLResolver resolver = new URLResolver();

    /**
     * init
     */
    private URLResolver() {

    }

    /**
     * 获取实例
     *
     * @return {@link URLResolver}
     */
    public static URLResolver getInstance() {
        return resolver;
    }

    /**
     * 解析程序url
     *
     * @param factory   缓存工厂
     * @param methodKey 方法key
     * @param method    方法
     * @param prefixUrl url前缀
     * @return {@link MethodUrl}
     */
    public MethodUrl resolverUrl(ClientCacheFactory factory,
                                  String methodKey,
                                  Method method,
                                  String prefixUrl) {
        if (factory.hasMethodUrl(methodKey)) {
            return factory.getMethodUrl(methodKey);
        }
        MethodUrl methodUrl = factory.getRequestAnnotationLink().resolverRequestAnnotation(method, prefixUrl);
        if (Objects.isNull(methodUrl)) {
            methodUrl = new MethodUrl(prefixUrl, null, null);
        }
        factory.putIfAbsent(methodKey, methodUrl);
        return methodUrl;
    }

}
