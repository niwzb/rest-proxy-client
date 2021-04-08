package com.wzb.rest.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RequestCallback;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RestHttpEntityRequestCallback
 */
public class RestHttpEntityRequestCallback implements RequestCallback {

    private Logger logger = LoggerFactory.getLogger(RestHttpEntityRequestCallback.class);

    @Nullable
    private final Type responseType;

    private final List<HttpMessageConverter<?>> messageConverters;

    /**
     * rest http实体请求回调
     *
     * @param responseType      响应类型
     * @param messageConverters 消息转换器
     */
    public RestHttpEntityRequestCallback(@Nullable Type responseType, List<HttpMessageConverter<?>> messageConverters) {
        this.responseType = responseType;
        this.messageConverters = messageConverters;
    }

    @Override
    public void doWithRequest(ClientHttpRequest request) throws IOException {
        if (this.responseType != null) {
            List<MediaType> allSupportedMediaTypes = getMessageConverters().stream()
                    .filter(converter -> canReadResponse(this.responseType, converter))
                    .flatMap(this::getSupportedMediaTypes)
                    .distinct()
                    .sorted(MediaType.SPECIFICITY_COMPARATOR)
                    .collect(Collectors.toList());
            if (logger.isDebugEnabled()) {
                logger.debug("Accept=" + allSupportedMediaTypes);
            }
            request.getHeaders().setAccept(allSupportedMediaTypes);
        }
    }

    /**
     * 获取消息转换器
     *
     * @return {@link List<HttpMessageConverter<?>>}
     */
    protected List<HttpMessageConverter<?>> getMessageConverters() {
        return messageConverters;
    }

    /**
     * 可读取响应
     *
     * @param responseType 响应类型
     * @param converter    转换器
     * @return boolean
     */
    private boolean canReadResponse(Type responseType, HttpMessageConverter<?> converter) {
        Class<?> responseClass = (responseType instanceof Class ? (Class<?>) responseType : null);
        if (responseClass != null) {
            return converter.canRead(responseClass, null);
        } else if (converter instanceof GenericHttpMessageConverter) {
            GenericHttpMessageConverter<?> genericConverter = (GenericHttpMessageConverter<?>) converter;
            return genericConverter.canRead(responseType, null, null);
        }
        return false;
    }

    /**
     * 获取支持的媒体类型
     *
     * @param messageConverter 消息转换器
     * @return {@link Stream<MediaType>}
     */
    private Stream<MediaType> getSupportedMediaTypes(HttpMessageConverter<?> messageConverter) {
        return messageConverter.getSupportedMediaTypes()
                .stream()
                .map(mediaType -> {
                    if (mediaType.getCharset() != null) {
                        return new MediaType(mediaType.getType(), mediaType.getSubtype());
                    }
                    return mediaType;
                });
    }
}
