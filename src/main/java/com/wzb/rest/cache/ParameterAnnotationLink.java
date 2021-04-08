package com.wzb.rest.cache;

import java.lang.annotation.Annotation;
import java.util.Objects;

/**
 * 参数注解链
 * @param <T> 参数注解
 * @param <U> 注解类型
 */
public class ParameterAnnotationLink<T, U extends Annotation> {

    private ParameterAnnotation<U> parameterAnnotation;

    private ParameterAnnotationLink<T, Annotation> next;

    /**
     * 解析参数注解
     *
     * @param annotation     参数注解
     * @param parameterIndex 参数索引
     * @param parameterName  参数名称
     * @param parameterType  参数类型
     * @return {@link ParameterSort}
     */
    @SuppressWarnings("unchecked")
    public ParameterSort resolverAnnotation(Annotation annotation,
                                            int parameterIndex,
                                            String parameterName,
                                            Class<?> parameterType) {
        String annotationValue = "";
        String annotationPath = null;
        Class<U> annotationClass = parameterAnnotation.getAnnotationClass();
        if (annotation.annotationType().equals(annotationClass)) {
            if (Objects.nonNull(parameterAnnotation.getPropertyFunction())) {
                annotationValue = parameterAnnotation.getPropertyFunction().apply((U) annotation).trim();
            }
            if (Objects.nonNull(parameterAnnotation.getFileFunction())) {
                annotationPath = parameterAnnotation.getFileFunction().apply((U) annotation).trim();
            }
            return ParameterSort.builder()
                    .index(parameterIndex)
                    .name(annotationValue.isEmpty() ? parameterName : annotationValue)
                    .type(parameterAnnotation.getParameterType())
                    .annotationName(annotationClass.getSimpleName())
                    .clazz(parameterType)
                    .path(annotationPath)
                    .build();
        }
        return Objects.nonNull(next)
                ? next.resolverAnnotation(annotation, parameterIndex, parameterName, parameterType)
                : null;
    }

    /**
     * 建造
     *
     * @param parameterAnnotations 参数注解
     * @return {@link ParameterAnnotationLink}
     */
    static ParameterAnnotationLink build(ParameterAnnotation... parameterAnnotations) {
        ParameterAnnotationLink link = new ParameterAnnotationLink();
        link.parameterAnnotation = parameterAnnotations[0];
        if (parameterAnnotations.length > 1) {
            link.next = new ParameterAnnotationLink();
        }
        ParameterAnnotationLink next = link.next;
        for (int i = 1; i < parameterAnnotations.length; i++) {
            next.parameterAnnotation = parameterAnnotations[i];
            if (i < parameterAnnotations.length - 1) {
                next.next = new ParameterAnnotationLink();
                next = next.next;
            }
        }
        return link;
    }
}
