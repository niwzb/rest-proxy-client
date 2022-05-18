package com.wzb.rest.config;

import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * MessageConvertConfigure
 */
@Configuration
public class MessageConvertConfigure {

    //请求数据converter
    private List<HttpMessageConverter<?>> requestHttpMessageConverter;

    //响应数据converter
    private List<HttpMessageConverter<?>> responseHttpMessageConverter;

    /**
     * 请求FastJsonHttpMessageConverter
     * @return FastJsonHttpMessageConverter
     */
    public HttpMessageConverter defaultRequestHttpMessageConverter() {
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        SerializerFeature[] serializerFeatures = new SerializerFeature[]{
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.DisableCircularReferenceDetect};
        SerializeConfig serializeConfig = SerializeConfig.globalInstance;
        fastJsonConfig.setSerializeConfig(serializeConfig);
        fastJsonConfig.setSerializerFeatures(serializerFeatures);
        fastJsonConfig.setCharset(StandardCharsets.UTF_8);
        fastConverter.setFastJsonConfig(fastJsonConfig);
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        mediaTypes.add(new MediaType("application", "*+json", StandardCharsets.UTF_8));
        fastConverter.setSupportedMediaTypes(mediaTypes);
        return fastConverter;
    }

    /**
     * 响应FastJsonHttpMessageConverter
     * @return FastJsonHttpMessageConverter
     */
    public HttpMessageConverter defaultResponseHttpMessageConverter() {
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        SerializerFeature[] serializerFeatures = new SerializerFeature[]{
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullBooleanAsFalse,
                SerializerFeature.DisableCircularReferenceDetect};
        SerializeConfig serializeConfig = SerializeConfig.globalInstance;
        fastJsonConfig.setSerializeConfig(serializeConfig);
        fastJsonConfig.setSerializerFeatures(serializerFeatures);
        fastJsonConfig.setCharset(StandardCharsets.UTF_8);
        fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
        fastConverter.setFastJsonConfig(fastJsonConfig);
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        mediaTypes.add(new MediaType("application", "*+json", StandardCharsets.UTF_8));
        fastConverter.setSupportedMediaTypes(mediaTypes);
        return fastConverter;
    }

    /**
     * 获取请求http消息转换器
     *
     * @return {@link List<HttpMessageConverter<?>>}
     */
    public List<HttpMessageConverter<?>> getRequestHttpMessageConverter() {
        return requestHttpMessageConverter;
    }

    /**
     * 获取响应http消息转换器
     *
     * @return {@link List<HttpMessageConverter<?>>}
     */
    public List<HttpMessageConverter<?>> getResponseHttpMessageConverter() {
        return responseHttpMessageConverter;
    }

    /**
     * 初始化
     */
    @PostConstruct
    private void init() {
        requestHttpMessageConverter = new LinkedList<>();
        responseHttpMessageConverter = new LinkedList<>();

        requestHttpMessageConverter.add(defaultRequestHttpMessageConverter());
        responseHttpMessageConverter.add(defaultResponseHttpMessageConverter());
    }
}
