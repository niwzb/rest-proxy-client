package com.wzb.rest.client;

import com.wzb.rest.request.RestAcceptHeaderRequestCallback;
import com.wzb.rest.response.RestResponseEntityResponseExtractor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * RestTemplateClient
 */
public class RestTemplateClient {

    private RestTemplate restTemplate;

    private List<HttpMessageConverter<?>> requestConverterList;

    private List<HttpMessageConverter<?>> responseConverterList;

    private List<ClientHttpRequestInterceptor> interceptorList;

    /**
     * rest模板客户端
     *
     * @param restTemplate       请求模板
     * @param requestConverters  请求转换器
     * @param responseConverters 响应转换器
     * @param interceptors       拦截器
     */
    public RestTemplateClient(RestTemplate restTemplate,
                              List<HttpMessageConverter<?>> requestConverters,
                              List<HttpMessageConverter<?>> responseConverters,
                              List<ClientHttpRequestInterceptor> interceptors) {
        this.restTemplate = restTemplate;
        init(requestConverters, responseConverters, interceptors);
    }

    /**
     * 交换
     *
     * @param url           网址
     * @param method        方法
     * @param requestEntity 请求实体
     * @param responseType  响应类型
     * @param <T>           泛型
     * @return {@link ResponseEntity<T>}
     */
    public <T> ResponseEntity<T> exchange(URI url,
                                          HttpMethod method,
                                          @Nullable HttpEntity<?> requestEntity,
                                          ParameterizedTypeReference<T> responseType) {
        Type type = responseType.getType();
        RequestCallback requestCallback = httpEntityCallback(requestEntity, type);
        ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(type);
        return nonNull(restTemplate.execute(url, method, requestCallback, responseExtractor));
    }

    /**
     * http实体回调
     *
     * @param requestBody  请求正文
     * @param responseType 响应类型
     * @return {@link RequestCallback}
     */
    private RequestCallback httpEntityCallback(@Nullable Object requestBody, Type responseType) {
        return new RestAcceptHeaderRequestCallback(requestBody, responseType, this.requestConverterList);
    }

    /**
     * 响应实体提取器
     *
     * @param responseType 响应类型
     * @param <T>          泛型
     * @return {@link ResponseExtractor<ResponseEntity<T>>}
     */
    private <T> ResponseExtractor<ResponseEntity<T>> responseEntityExtractor(Type responseType) {
        return new RestResponseEntityResponseExtractor<>(responseType, this.responseConverterList);
    }

    /**
     * 非空
     *
     * @param result 结果
     * @param <T>    泛型
     * @return {@link T}
     */
    private static <T> T nonNull(@Nullable T result) {
        Assert.state(result != null, "No result");
        return result;
    }

    /**
     * rest模板客户端初始化
     *
     * @param requestConverters  请求转换器
     * @param responseConverters 响应转换器
     * @param interceptors       拦截器
     */
    private void init(List<HttpMessageConverter<?>> requestConverters,
                      List<HttpMessageConverter<?>> responseConverters,
                      List<ClientHttpRequestInterceptor> interceptors) {

        if (null == restTemplate) {
            restTemplate = new RestTemplate();
        }

        this.requestConverterList = newArrayList(restTemplate.getMessageConverters());
        if (null != requestConverters && !requestConverters.isEmpty()) {
            Set<Class<?>> classSet = requestConverters.stream().map(HttpMessageConverter::getClass).collect(toSet());
            this.requestConverterList.removeIf(converter -> classSet.contains(converter.getClass()));
            this.requestConverterList.addAll(requestConverters);
        }

        this.responseConverterList = newArrayList(restTemplate.getMessageConverters());
        if (null != responseConverters && !responseConverters.isEmpty()) {
            Set<Class<?>> classSet = responseConverters.stream().map(HttpMessageConverter::getClass).collect(toSet());
            this.responseConverterList.removeIf(converter -> classSet.contains(converter.getClass()));
            this.responseConverterList.addAll(responseConverters);
        }

        this.interceptorList = newArrayList(restTemplate.getInterceptors());

        if (null != interceptors && !interceptors.isEmpty()) {
            Set<Class<?>> classSet = interceptors.stream().map(ClientHttpRequestInterceptor::getClass).collect(toSet());
            this.interceptorList.removeIf(converter -> classSet.contains(converter.getClass()));
            this.interceptorList.addAll(interceptors);
            restTemplate.setInterceptors(interceptorList);
        }
    }

    /**
     * 新建数组列表
     *
     * @param oldList 旧列表
     * @param <T>     类型
     * @return {@link List<?>}
     */
    private <T> List<T> newArrayList(List<T> oldList) {
        return null == oldList ? new ArrayList<>() : new ArrayList<>(oldList);
    }
}
