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

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static ParameterResolver resolver = new ParameterResolver();

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
        if (!factory.hasParameterSort(methodKey)) {
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
                final int parameterIndex = i;
                final Class<?> parameterType = parameter.getType();
                @SuppressWarnings("unchecked")
                Optional<ParameterSort> optional = Stream.of(paramAnn).map(annotation ->
                        link.resolverAnnotation(annotation, parameterIndex, parameterName, parameterType))
                        .filter(Objects::nonNull).findFirst();
                if (optional.isPresent()) {
                    ParameterSort parameterSort = optional.get();
                    factory.putIfAbsent(methodKey, parameterSort);
                    factory.putIfAbsent(methodKey, parameterSort.getPath());
                    logger.debug("found annotation's @{} at method parameter index {}",
                            parameterSort.getAnnotationName(),
                            i);
                }
            }
        }
    }

    /**
     * 解析程序参数类型
     *
     * @param factory   缓存工厂
     * @param methodKey 方法键
     */
    public void resolverParameterType(ClientCacheFactory factory, String methodKey) {
        List<ParameterSort> parameterSortList = factory.getParameterSort(methodKey);
        if (null != parameterSortList && !parameterSortList.isEmpty()) {
            for (ParameterType parameterType : ParameterType.values()) {
                factory.putIfAbsent(methodKey, parameterType, parameterSortList.stream()
                        .filter(sort -> sort.getType() == parameterType).collect(Collectors.toList()));
            }
        }
    }
}
