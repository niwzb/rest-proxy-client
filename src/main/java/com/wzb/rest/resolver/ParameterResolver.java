package com.wzb.rest.resolver;

import com.wzb.rest.cache.ClientCacheFactory;
import com.wzb.rest.cache.ParameterAnnotationLink;
import com.wzb.rest.cache.ParameterSort;
import com.wzb.rest.cache.ParameterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 参数解析
 */
public final class ParameterResolver {

    private static final Logger logger = LoggerFactory.getLogger(ParameterResolver.class);

    private static final ParameterResolver resolver = new ParameterResolver();

    /**
     * init
     */
    private ParameterResolver() {

    }

    /**
     * 获取实例
     *
     * @return {@link ParameterResolver}
     */
    public static ParameterResolver getInstance() {
        return resolver;
    }

    /**
     * 解析方法参数
     *
     * @param factory     缓存工厂
     * @param methodKey   方法key
     * @param annotations 注释
     * @param parameters  参数
     */
    public void resolverParameter(ClientCacheFactory factory,
                                  String methodKey,
                                  Annotation[][] annotations,
                                  Parameter[] parameters) {
        ParameterSort requestBody;
        ParameterAnnotationLink link = factory.getParameterAnnotationLink();
        Parameter parameter;
        for (int i = 0; i < annotations.length; i++) {
            parameter = parameters[i];
            //如果注解没有指定则默认取方法参数名称
            String parameterName = parameter.getName();
            Annotation[] paramAnn = annotations[i];
            //参数没有注解 默认放请求体
            if (paramAnn.length == 0) {
                requestBody = ParameterSort.builder()
                        .index(i)
                        .name(parameterName)
                        .type(ParameterType.BODY)
                        .clazz(parameter.getType())
                        .build();
                factory.putIfAbsent(methodKey, requestBody);
                continue;
            }
            for (Annotation annotation : paramAnn) {
                ParameterSort sort = link.resolverAnnotation(annotation, i, parameterName, parameter.getType());
                if (null != sort) {
                    factory.putIfAbsent(methodKey, sort);
                    factory.putIfAbsent(methodKey, sort.getPath());
                    logger.debug("found annotation's @{} at method parameter index {}", sort.getAnnotationName(), i);
                    break;
                }
            }
        }
    }
}
