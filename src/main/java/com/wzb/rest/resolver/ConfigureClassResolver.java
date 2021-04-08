package com.wzb.rest.resolver;

import com.wzb.rest.annation.FailBackResponse;
import com.wzb.rest.annation.LogBack;
import com.wzb.rest.annation.NullResponse;
import com.wzb.rest.cache.ClientCacheFactory;
import com.wzb.rest.log.RestClientLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * 配置类解析
 */
public final class ConfigureClassResolver {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static ConfigureClassResolver resolver = new ConfigureClassResolver();

    private static ApplicationContext applicationContext;

    /**
     * init
     */
    private ConfigureClassResolver() {

    }

    /**
     * 获取实例
     *
     * @return {@link ConfigureClassResolver}
     */
    public static ConfigureClassResolver getInstance() {
        return resolver;
    }

    /**
     * 解析配置
     *
     * @param configureClass     配置类
     * @param requestConverters  请求转换器
     * @param responseConverters 响应转换器
     * @param interceptors       拦截器
     * @param factory            缓存工厂
     * @param context            spring容器
     */
    public void resolver(Class<?>[] configureClass,
                         List<HttpMessageConverter<?>> requestConverters,
                         List<HttpMessageConverter<?>> responseConverters,
                         List<ClientHttpRequestInterceptor> interceptors,
                         ClientCacheFactory factory,
                         ApplicationContext context) {
        setApplicationContext(context);
        if (null != configureClass && configureClass.length > 0) {
            List<Class<?>> classList = Stream.of(configureClass).filter(Objects::nonNull).collect(toList());
            Map<Class<?>, List<Method>> methodMap = new HashMap<>();
            classList.forEach(clazz -> {
                Method[] methods = clazz.getMethods();
                List<Method> methodList = Stream.of(methods).filter(method -> {
                    Bean beanAnnotation = method.getAnnotation(Bean.class);
                    NullResponse nullResponse = method.getAnnotation(NullResponse.class);
                    FailBackResponse failBackResponse = method.getAnnotation(FailBackResponse.class);
                    LogBack logBack = method.getAnnotation(LogBack.class);
                    return beanAnnotation != null || nullResponse != null
                            || failBackResponse != null || logBack != null;
                }).collect(toList());
                if (!methodList.isEmpty()) {
                    methodMap.put(clazz, methodList);
                }
            });

            if (!methodMap.isEmpty()) {
                methodMap.forEach((key, value) -> this.resolverMethod(key, value,
                        requestConverters, responseConverters, interceptors, factory));
            }
        }
    }

    /**
     * 解析方法
     *
     * @param configureClass     配置类
     * @param methodList         方法列表
     * @param requestConverters  请求转换器
     * @param responseConverters 响应转换器
     * @param interceptors       拦截器
     * @param factory            缓存工厂
     */
    private void resolverMethod(Class<?> configureClass,
                                List<Method> methodList,
                                List<HttpMessageConverter<?>> requestConverters,
                                List<HttpMessageConverter<?>> responseConverters,
                                List<ClientHttpRequestInterceptor> interceptors,
                                ClientCacheFactory factory) {
        try {
            Object instance = configureClass.newInstance();
            methodList.forEach(method -> this.resolverMethod(instance, method,
                    requestConverters, responseConverters, interceptors, factory));
        } catch (InstantiationException | IllegalAccessException e) {
            logger.warn("create instance of class[{}] error", configureClass.getName());
        }

    }

    /**
     * 解析方法
     *
     * @param instance           配置类实例
     * @param method             方法
     * @param requestConverters  请求转换器
     * @param responseConverters 响应转换器
     * @param interceptors       拦截器
     * @param factory            缓存工厂
     */
    private void resolverMethod(Object instance,
                                Method method,
                                List<HttpMessageConverter<?>> requestConverters,
                                List<HttpMessageConverter<?>> responseConverters,
                                List<ClientHttpRequestInterceptor> interceptors,
                                ClientCacheFactory factory) {
        Bean beanAnnotation = method.getAnnotation(Bean.class);
        LogBack logBack = method.getAnnotation(LogBack.class);
        if (null != beanAnnotation) {
            resolverBeanAnnotation(instance, method, requestConverters, responseConverters, interceptors);
        } else if (null != logBack) {
            resolverLogBackAnnotation(instance, method, factory);
        } else {
            resolverResponseAnnotation(instance, method, factory);
        }
    }

    /**
     * 解析程序响应注解
     *
     * @param instance 实例
     * @param method   方法
     * @param factory  工厂
     */
    private void resolverResponseAnnotation(Object instance,
                                            Method method,
                                            ClientCacheFactory factory) {
        Class<?> beanClass = method.getReturnType();
        NullResponse nullResponse = method.getAnnotation(NullResponse.class);
        FailBackResponse failBackResponse = method.getAnnotation(FailBackResponse.class);
        try {
            //空响应
            if (null != nullResponse) {
                if (method.getParameterCount() == 0) {
                    Object bean = method.invoke(instance);
                    factory.putNullResponseIfAbsent(beanClass, bean);
                } else {
                    factory.putNullResponseMethodIfAbsent(beanClass, method, instance);
                }
            }
            //异常降级
            if (null != failBackResponse) {
                if (method.getParameterCount() == 0) {
                    Object bean = method.invoke(instance);
                    factory.putFailBackResponseIfAbsent(beanClass, bean);
                } else {
                    factory.putFailBackResponseMethodIfAbsent(beanClass, method, instance);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.warn("create bean {} at class[{}] error", beanClass.getName(), instance.getClass().getName());
        }
    }

    /**
     * 分解器bean对象注解
     *
     * @param instance           实例
     * @param method             方法
     * @param requestConverters  请求转换器
     * @param responseConverters 响应转换器
     * @param interceptors       拦截器
     */
    private void resolverBeanAnnotation(Object instance,
                                        Method method,
                                        List<HttpMessageConverter<?>> requestConverters,
                                        List<HttpMessageConverter<?>> responseConverters,
                                        List<ClientHttpRequestInterceptor> interceptors) {
        Class<?> beanClass = method.getReturnType();
        try {
            Object bean = method.invoke(instance, getParameterFromContext(method));
            Bean beanAnnotation = method.getAnnotation(Bean.class);
            //拦截器
            if (ClientHttpRequestInterceptor.class.isAssignableFrom(beanClass)) {
                interceptors.removeIf(interceptor -> interceptor.getClass().equals(bean.getClass()));
                interceptors.add((ClientHttpRequestInterceptor) bean);
            } else if (HttpMessageConverter.class.isAssignableFrom(beanClass)) {
                //消息转换器
                String beanName = beanAnnotation.value()[0];
                resolverHttpMessageConverter((HttpMessageConverter<?>) bean, beanName,
                        requestConverters, responseConverters);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.warn("create bean {} at class[{}] error", beanClass.getName(), instance.getClass().getName());
        }
    }

    /**
     * 分解器日志对象注解
     *
     * @param instance 实例
     * @param method   方法
     * @param factory  工厂
     */
    private void resolverLogBackAnnotation(Object instance,
                                           Method method,
                                           ClientCacheFactory factory) {
        LogBack logBackAnnotation = method.getAnnotation(LogBack.class);
        // 日志注解
        if (null != logBackAnnotation
                && method.getParameterCount() == 1
                && RestClientLog.class.isAssignableFrom(method.getParameterTypes()[0])) {
            factory.setLogBackMethodIfAbsent(method, instance);
        }
    }

    /**
     * 解析http消息转换器
     *
     * @param converter          转换器
     * @param beanName           bean对象名称
     * @param requestConverters  请求转换器
     * @param responseConverters 响应转换器
     */
    private void resolverHttpMessageConverter(HttpMessageConverter<?> converter,
                                              String beanName,
                                              List<HttpMessageConverter<?>> requestConverters,
                                              List<HttpMessageConverter<?>> responseConverters) {
        if (beanName.startsWith("request")) {
            requestConverters.removeIf(exits -> exits.getClass().equals(converter.getClass()));
            requestConverters.add(converter);
        } else if (beanName.startsWith("response")) {
            responseConverters.removeIf(exits -> exits.getClass().equals(converter.getClass()));
            responseConverters.add(converter);
        }
    }

    /**
     * 设置应用程序上下文
     *
     * @param context 上下文
     */
    private void setApplicationContext(ApplicationContext context) {
        if (null == applicationContext) {
            synchronized (ConfigureClassResolver.class) {
                if (null == applicationContext) {
                    applicationContext = context;
                }
            }
        }
    }

    /**
     * 从上下文获取参数
     *
     * @param method 方法
     * @return {@link Object[]}
     */
    private Object[] getParameterFromContext(Method method) {
        Object[] parameters = new Object[method.getParameterCount()];
        Class<?>[] parameterClass = method.getParameterTypes();
        for (int i = 0; i < parameterClass.length; i++) {
            parameters[i] = applicationContext.getBean(parameterClass[i]);
        }
        return parameters;
    }
}
