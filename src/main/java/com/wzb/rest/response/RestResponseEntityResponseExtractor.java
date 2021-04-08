package com.wzb.rest.response;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResponseExtractor;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * RestResponseEntityResponseExtractor
 *
 * @param <T> 泛型
 */
public class RestResponseEntityResponseExtractor<T> implements ResponseExtractor<ResponseEntity<T>> {

    @Nullable
    private final HttpMessageConverterExtractor<T> delegate;

    /**
     * 响应实体响应提取器
     *
     * @param responseType         响应类型
     * @param messageConverters http消息转换器
     */
    public RestResponseEntityResponseExtractor(@Nullable Type responseType,
                                               List<HttpMessageConverter<?>> messageConverters) {
        if (responseType != null && Void.class != responseType) {
            this.delegate = new HttpMessageConverterExtractor<>(responseType, messageConverters);
        } else {
            this.delegate = null;
        }
    }

    @Override
    public ResponseEntity<T> extractData(ClientHttpResponse response) throws IOException {
        if (this.delegate != null) {
            T body = this.delegate.extractData(response);
            return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).body(body);
        } else {
            return ResponseEntity.status(response.getRawStatusCode()).headers(response.getHeaders()).build();
        }
    }
}
