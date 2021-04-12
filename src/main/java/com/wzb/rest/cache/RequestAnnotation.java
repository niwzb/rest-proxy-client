package com.wzb.rest.cache;

import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.util.function.Function;

/**
 * 请求注解
 *
 * @param <T> 注解类型
 */
public class RequestAnnotation<T extends Annotation> {

    private Class<T> annotationClass;
    private Function<T, String[]> valueFunction;
    private Function<T, String[]> pathFunction;
    private Function<T, String[]> producesFunction;
    private Function<T, RequestMethod[]> httpMethodFunction;
    private HttpMethod defaultHttpMethod;

    public Class<T> getAnnotationClass() {
        return annotationClass;
    }

    public Function<T, String[]> getValueFunction() {
        return valueFunction;
    }

    public Function<T, String[]> getPathFunction() {
        return pathFunction;
    }

    public Function<T, RequestMethod[]> getHttpMethodFunction() {
        return httpMethodFunction;
    }

    public HttpMethod getDefaultHttpMethod() {
        return defaultHttpMethod;
    }

    public Function<T, String[]> getProducesFunction() {
        return producesFunction;
    }

    public static <T extends Annotation> Builder<T> builder(Class<T> clazz) {
        return new Builder<T>().annotationClass(clazz);
    }

    /**
     * 建设者
     *
     * @param <T> 泛型
     */
    public static class Builder<T extends Annotation> {

        private Class<T> annotationClass;
        private Function<T, String[]> valueFunction;
        private Function<T, String[]> pathFunction;
        private Function<T, String[]> producesFunction;
        private Function<T, RequestMethod[]> httpMethodFunction;
        private HttpMethod defaultHttpMethod;

        public Builder<T> annotationClass(Class<T> annotationClass) {
            this.annotationClass = annotationClass;
            return this;
        }

        public Builder<T> valueFunction(Function<T, String[]> valueFunction) {
            this.valueFunction = valueFunction;
            return this;
        }

        public Builder<T> pathFunction(Function<T, String[]> pathFunction) {
            this.pathFunction = pathFunction;
            return this;
        }

        public Builder<T> producesFunction(Function<T, String[]> producesFunction) {
            this.producesFunction = producesFunction;
            return this;
        }

        public Builder<T> httpMethodFunction(Function<T, RequestMethod[]> httpMethodFunction) {
            this.httpMethodFunction = httpMethodFunction;
            return this;
        }

        public Builder<T> defaultHttpMethod(HttpMethod defaultHttpMethod) {
            this.defaultHttpMethod = defaultHttpMethod;
            return this;
        }

        /**
         * 建造
         *
         * @return {@link RequestAnnotation}
         */
        public RequestAnnotation<T> build() {
            RequestAnnotation<T> requestAnnotation = new RequestAnnotation<>();
            requestAnnotation.annotationClass = this.annotationClass;
            requestAnnotation.defaultHttpMethod = this.defaultHttpMethod;
            requestAnnotation.valueFunction = this.valueFunction;
            requestAnnotation.pathFunction = this.pathFunction;
            requestAnnotation.httpMethodFunction = this.httpMethodFunction;
            requestAnnotation.producesFunction = this.producesFunction;
            return requestAnnotation;
        }
    }
}
