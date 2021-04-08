package com.wzb.rest.cache;

import java.lang.annotation.Annotation;
import java.util.function.Function;

/**
 * 参数注解
 *
 * @param <T> 注解类型
 */
public class ParameterAnnotation<T extends Annotation> {

    private Class<T> annotationClass;

    private ParameterType parameterType;

    private Function<T, String> propertyFunction;

    private Function<T, String> fileFunction;

    /**
     * 参数注解
     *
     * @param annotationClass 注解类
     * @param parameterType   参数类型
     */
    public ParameterAnnotation(Class<T> annotationClass, ParameterType parameterType) {
        this.annotationClass = annotationClass;
        this.parameterType = parameterType;
    }

    /**
     * 参数注解
     *
     * @param annotationClass  注解类
     * @param parameterType    参数类型
     * @param propertyFunction 属性
     */
    public ParameterAnnotation(Class<T> annotationClass,
                               ParameterType parameterType,
                               Function<T, String> propertyFunction) {
        this(annotationClass, parameterType, propertyFunction, null);
    }

    /**
     * 参数注解
     *
     * @param annotationClass  注解类
     * @param parameterType    参数类型
     * @param propertyFunction 属性
     * @param fileFunction     文件属性
     */
    public ParameterAnnotation(Class<T> annotationClass,
                               ParameterType parameterType,
                               Function<T, String> propertyFunction,
                               Function<T, String> fileFunction) {
        this.annotationClass = annotationClass;
        this.parameterType = parameterType;
        this.propertyFunction = propertyFunction;
        this.fileFunction = fileFunction;
    }

    public Class<T> getAnnotationClass() {
        return annotationClass;
    }

    public ParameterType getParameterType() {
        return parameterType;
    }

    public Function<T, String> getPropertyFunction() {
        return propertyFunction;
    }

    public Function<T, String> getFileFunction() {
        return fileFunction;
    }
}
