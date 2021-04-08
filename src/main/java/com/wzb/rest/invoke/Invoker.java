package com.wzb.rest.invoke;

import com.alibaba.fastjson.JSON;
import com.wzb.rest.cache.ClientCacheFactory;
import com.wzb.rest.cache.DynamicParameter;
import com.wzb.rest.cache.ParameterSort;
import com.wzb.rest.client.RestTemplateClient;
import com.wzb.rest.exception.FileException;
import com.wzb.rest.log.RestClientLog;
import com.wzb.rest.cache.ParameterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 执行者
 */
public final class Invoker {

    private static Logger logger = LoggerFactory.getLogger(Invoker.class);

    private static ClientCacheFactory factory = ClientCacheFactory.getInstance();

    private static final String GETTER = "get";

    private static final int READ_LEN = 1024;

    private static final int NOT_FOUND = -1;

    /**
     * inti
     */
    private Invoker() {

    }

    /**
     * 代理执行
     *
     * @param methodKey  方法key
     * @param args       方法参
     * @param returnType 返回类型
     * @return {@link Object}
     */
    public static Object invoke(String methodKey, List<Object> args, Class<?> returnType) {
        long start = System.currentTimeMillis();
        //解析链接
        HttpMethod httpMethod = factory.getMethodUrl(methodKey).getHttpMethod();
        //不识别请求类型
        if (null == httpMethod) {
            //mock
            return null;
        }
        //生成URL
        String callUrl = generateURL(methodKey, args);
        //远程调用
        Object response = call(methodKey, callUrl, httpMethod, args, factory.getRestTemplateClient());
        if (logger.isDebugEnabled()) {
            logger.debug(">>>>>>>>>>>>>>>>>>rest-client-proxy invoke cost:{}(ms)", System.currentTimeMillis() - start);
        }
        return response;
    }

    /**
     * 代理执行
     *
     * @param methodKey 方法key
     * @param args      方法参
     */
    public static void invoke(String methodKey, List<Object> args) {
        long start = System.currentTimeMillis();
        //解析链接
        HttpMethod httpMethod = factory.getMethodUrl(methodKey).getHttpMethod();
        //识别请求类型
        if (null != httpMethod) {
            //生成URL
            String callUrl = generateURL(methodKey, args);
            //远程调用
            call(methodKey, callUrl, httpMethod, args, factory.getRestTemplateClient());
            if (logger.isDebugEnabled()) {
                logger.debug(">>>>>>>>>>>>>>>>>>rest-client-proxy invoke cost:{}(ms)",
                        System.currentTimeMillis() - start);
            }
        }
    }

    /**
     * 生成url
     *
     * @param methodKey 方法key
     * @param args      方法参数
     * @return {@link String}
     */
    private static String generateURL(String methodKey, List<Object> args) {
        //动态参数
        List<DynamicParameter> dynamicParameterList = factory.getDynamicParameter(methodKey);
        if (!factory.hasParameterSort(methodKey) || factory.getParameterSort(methodKey).isEmpty()) {
            //处理URL动态参数
            return parameterReplacePlaceholder(dynamicParameterList);
        }
        //param参数
        List<ParameterSort> parameterSortList = factory.getParameterSortByParameterType(methodKey, ParameterType.PARAM);
        //url动态参数
        List<ParameterSort> pathParameterSortList = factory.getParameterSortByParameterType(methodKey, ParameterType.PATH);
        //处理URL动态参数
        String newUrl = parameterReplacePlaceholder(dynamicParameterList, pathParameterSortList, args);
        //param参数拼接
        StringBuilder parameter = new StringBuilder();
        //参数拼接到url里
        parameterSortList.forEach(sort -> {
            Object parameterValue = args.get(sort.getIndex());
            if (null != parameterValue) {
                if (parameter.length() > 0) {
                    parameter.append("&");
                }
                parameter.append(sort.getName()).append("=").append(convert(parameterValue));
            }
        });
        if (factory.hasPath(methodKey)) {
            if (parameter.length() > 0) {
                parameter.append("&");
            }
            parameter.append("path=").append(factory.getPath(methodKey));
        }
        if (parameter.length() == 0) {
            return newUrl;
        }
        if (!newUrl.contains("?")) {
            return newUrl.concat("?").concat(parameter.toString());
        }
        if (newUrl.endsWith("&")) {
            return newUrl.concat(parameter.toString());
        }
        return newUrl.concat("&").concat(parameter.toString());
    }

    /**
     * 参数替换占位符
     *
     * @param dynamicParameterList 动态参数表
     * @return {@link String}
     */
    private static String parameterReplacePlaceholder(List<DynamicParameter> dynamicParameterList) {
        return parameterReplacePlaceholder(dynamicParameterList, null, null);
    }

    /**
     * 参数替换占位符
     *
     * @param dynamicParameterList 动态参数表
     * @param parameterSortList    参数排序列表
     * @param args                 方法参数
     * @return {@link String}
     */
    private static String parameterReplacePlaceholder(List<DynamicParameter> dynamicParameterList,
                                                      List<ParameterSort> parameterSortList,
                                                      List<Object> args) {
        StringBuilder url = new StringBuilder();
        dynamicParameterList.forEach(dynamicParameter -> {
            url.append(dynamicParameter.getSubURL());
            String dynamicParameterName = dynamicParameter.getName();
            if (null != dynamicParameterName) {
                String dynamicParameterValue = findDynamicParameterValue(dynamicParameterName,
                        parameterSortList, args);
                url.append(dynamicParameterValue);
            }
        });
        return url.toString();
    }

    /**
     * 找到动态参数值
     *
     * @param dynamicParameterName 动态参数名称
     * @param parameterSortList    参数排序列表
     * @param args                 方法参数
     * @return {@link String}
     */
    private static String findDynamicParameterValue(String dynamicParameterName,
                                                    List<ParameterSort> parameterSortList,
                                                    List<Object> args) {
        if (null == parameterSortList || parameterSortList.isEmpty()) {
            return "";
        }
        //参数名相同
        Optional<ParameterSort> optional = parameterSortList.stream()
                .filter(sort -> Objects.equals(sort.getName(), dynamicParameterName)).findFirst();
        if (optional.isPresent()) {
            ParameterSort sort = optional.get();
            Object value = args.get(sort.getIndex());
            return convert(value, "");
        }
        return "";
    }

    /**
     * 类型转换
     *
     * @param parameter 类型
     * @return 结果
     */
    private static String convert(Object parameter) {
        return convert(parameter, null);
    }

    /**
     * 类型转换
     *
     * @param parameter   类型
     * @param nullDefault 空默认值
     * @return 结果
     */
    private static String convert(Object parameter, String nullDefault) {
        if (Objects.isNull(parameter)) {
            return nullDefault;
        }
        if (parameter.getClass().isArray() || parameter instanceof Collection) {
            return JSON.toJSONString(parameter);
        }
        if (parameter instanceof Date) {
            return String.valueOf(((Date) parameter).getTime());
        }
        if (isNotBeanOrMap(parameter.getClass())) {
            return String.valueOf(parameter);
        }
        return JSON.toJSONString(parameter);
    }

    /**
     * 转map
     *
     * @param parameter 参数
     * @param <T>       泛型
     * @return Map
     */
    private static <T> Map<String, String> toMap(T parameter) {
        Map<String, String> parameterMap = new HashMap<>();
        if (null == parameter) {
            return parameterMap;
        }
        if (parameter instanceof Map) {
            ((Map<?, ?>) parameter).forEach((k, v) ->
                    parameterMap.put(String.valueOf(k), convert(v)));
        } else {
            Method[] methods = parameter.getClass().getMethods();
            //过滤getter方法
            Stream.of(methods).filter(method ->
                    method.getName().startsWith(GETTER)
                            && !method.getReturnType().equals(Void.class)
                            && method.getParameterCount() == 0
            ).forEach(method -> {
                String propertyName = method.getName().substring(GETTER.length());
                String first = String.valueOf(propertyName.charAt(0)).toLowerCase();
                propertyName = first.concat(propertyName.substring(1));
                try {
                    Object propertyValue = method.invoke(parameter);
                    parameterMap.put(propertyName, convert(propertyValue));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.warn(String.format("invoke method's %s error", method.getName()), e);
                }
            });
        }
        return parameterMap;
    }

    /**
     * 调用
     *
     * @param methodKey    方法key
     * @param callUrl      调用url
     * @param httpMethod   http方法
     * @param args         方法参数
     * @param restTemplate 请求模板
     * @return Response
     */
    private static Object call(String methodKey,
                               String callUrl,
                               HttpMethod httpMethod,
                               List<Object> args,
                               RestTemplateClient restTemplate) {
        Object response = null;
        ResponseEntity<?> responseEntity;
        HttpEntity<?> httpEntity = null;
        Class<?> responseClass = factory.getResponseClass(methodKey).get(0);
        try {
            httpEntity = buildHttpEntity(factory, methodKey, args);
            long ii = System.currentTimeMillis();
            responseEntity = restTemplate.exchange(new URI(callUrl), httpMethod, httpEntity,
                    buildParameterizedTypeReference(methodKey));
            response = responseEntity.getBody();
            //接口返回空
            if (null == response) {
                if (factory.hasNullResponseMethod(responseClass)) {
                    //使用配置的空响应方法
                    response = factory.getNullResponse(responseClass, callUrl);
                } else if (factory.hasNullResponse(responseClass)) {
                    //使用配置的空响应
                    response = factory.getNullResponse(responseClass);
                }
            }
            if (factory.hasLogBackMethod()) {
                factory.invokeLogBackMethod(RestClientLog.builder()
                        .url(callUrl)
                        .method(httpMethod)
                        .parameter(httpEntity)
                        .response(response)
                        .speedTime(System.currentTimeMillis() - ii)
                        .multipart(MediaType.MULTIPART_FORM_DATA.equals(httpEntity.getHeaders().getContentType()))
                        .build());
            }
        } catch (Exception e) {
            if (factory.hasLogBackMethod()) {
                factory.invokeLogBackMethod(RestClientLog.builder()
                        .url(callUrl)
                        .method(httpMethod)
                        .parameter(httpEntity)
                        .response(response)
                        .throwable(e)
                        .multipart(null != httpEntity && MediaType.MULTIPART_FORM_DATA
                                .equals(httpEntity.getHeaders().getContentType()))
                        .build());
            }
            //异常降级处理
            if (factory.hasFailBackResponseMethod(responseClass)) {
                response = factory.getFailBackResponse(responseClass, e, e.getLocalizedMessage());
            } else if (factory.hasFailBackResponse(responseClass)) {
                response = factory.getFailBackResponse(responseClass);
            }
        }
        return response;
    }

    /**
     * 构建参数化类型引用
     *
     * @param methodKey 方法键
     * @return {@link ParameterizedTypeReference}
     */
    private static ParameterizedTypeReference<?> buildParameterizedTypeReference(String methodKey) {
        return ParameterizedTypeReference.forType(factory.getReturnType(methodKey));
    }

    /**
     * 构建HttpEntity
     *
     * @param factory   缓存工厂
     * @param methodKey 方法key
     * @param args      方法参数
     * @return HttpEntity<?>
     * @throws FileException 文件异常
     */
    private static HttpEntity<?> buildHttpEntity(ClientCacheFactory factory,
                                                 String methodKey,
                                                 List<Object> args) throws FileException {
        List<ParameterSort> fileParameterList = factory.getParameterSortByParameterType(methodKey, ParameterType.FILE);
        HttpHeaders httpHeaders = buildHttpHeaders(factory, methodKey, args, !fileParameterList.isEmpty());
        List<ParameterSort> requestBodyList = factory.getParameterSortByParameterType(methodKey, ParameterType.BODY);
        List<ParameterSort> restRequestBodyList = factory.getParameterSortByParameterType(methodKey, ParameterType.REST);
        HttpEntity<?> httpEntity;
        if (!fileParameterList.isEmpty()) {
            //文件组装
            MultiValueMap<String, Object> multiValueMap = new LinkedMultiValueMap<>();
            for (ParameterSort sort : fileParameterList) {
                multiValueMap.add(sort.getName(), buildByteArrayResource(args.get(sort.getIndex()), sort.getName()));
            }
            final Map<String, Object> requestMap = new HashMap<>();
            //多个requestBody组装成一个map
            if (requestBodyList.size() > 1) {
                requestBodyList.forEach(body -> requestMap.put(body.getName(), convert(args.get(body.getIndex()))));
            } else if (!requestBodyList.isEmpty() && isNotBeanOrMap(requestBodyList.get(0).getClazz())) {
                requestMap.put(requestBodyList.get(0).getName(), args.get(requestBodyList.get(0).getIndex()));
            } else if (!requestBodyList.isEmpty()) {
                requestMap.putAll(toMap(args.get(requestBodyList.get(0).getIndex())));
            }
            requestMap.forEach(multiValueMap::add);
            httpEntity = new HttpEntity<>(multiValueMap, httpHeaders);
        } else if (requestBodyList.isEmpty()) {
            if (!restRequestBodyList.isEmpty()) {
                httpEntity = new HttpEntity<>(args.get(restRequestBodyList.get(0).getIndex()), httpHeaders);
            } else {
                httpEntity = new HttpEntity<>(httpHeaders);
            }
        } else if (requestBodyList.size() > 1) {
            //多个requestBody组装成一个map
            final Map<String, Object> requestMap = new HashMap<>();
            requestBodyList.forEach(body -> requestMap.put(body.getName(), args.get(body.getIndex())));
            httpEntity = new HttpEntity<>(requestMap, httpHeaders);
        } else if (isNotBeanOrMap(requestBodyList.get(0).getClazz())) {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put(requestBodyList.get(0).getName(), args.get(requestBodyList.get(0).getIndex()));
            httpEntity = new HttpEntity<>(bodyMap, httpHeaders);
        } else {
            Object singleBody = args.get(requestBodyList.get(0).getIndex());
            httpEntity = new HttpEntity<>(singleBody, httpHeaders);
        }
        return httpEntity;
    }

    /**
     * 生成http标头
     *
     * @param factory   缓存工厂
     * @param methodKey 方法key
     * @param args      方法参数
     * @param hasFile   有文件
     * @return {@link HttpHeaders}
     */
    private static HttpHeaders buildHttpHeaders(ClientCacheFactory factory,
                                                String methodKey,
                                                List<Object> args,
                                                boolean hasFile) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (hasFile) {
            //文件流
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        } else {
            httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        }
        List<ParameterSort> headerParameterSortList = factory.getParameterSortByParameterType(methodKey, ParameterType.HEADER);
        if (!headerParameterSortList.isEmpty()) {
            headerParameterSortList.forEach(sort ->
                    httpHeaders.add(sort.getName(), convert(args.get(sort.getIndex()))));
        }
        return httpHeaders;
    }

    /**
     * 不是bean对象或map
     *
     * @param clazz 类型
     * @return boolean
     */
    private static boolean isNotBeanOrMap(Class<?> clazz) {
        return clazz.isArray() || clazz.isPrimitive() || clazz.isEnum()
                || clazz.equals(Byte.class) || clazz.equals(Character.class)
                || clazz.equals(Short.class) || clazz.equals(Integer.class)
                || clazz.equals(Long.class) || clazz.equals(Float.class)
                || clazz.equals(Double.class) || clazz.equals(Boolean.class)
                || Collection.class.isAssignableFrom(clazz)
                || CharSequence.class.isAssignableFrom(clazz);
    }


    /**
     * 生成字节数组资源
     *
     * @param object          对象
     * @param defaultFileName 默认文件名
     * @return {@link ByteArrayResource}
     * @throws FileException 文件异常
     */
    private static ByteArrayResource buildByteArrayResource(Object object, String defaultFileName)
            throws FileException {
        if (object instanceof byte[]) {
            return new ByteArrayResource((byte[]) object) {
                @Override
                public String getFilename() {
                    return defaultFileName;
                }
            };
        }
        try {
            if (object instanceof MultipartFile) {
                MultipartFile multipartFile = (MultipartFile) object;
                return new ByteArrayResource(multipartFile.getBytes()) {
                    @Override
                    public String getFilename() {
                        return Optional.ofNullable(multipartFile.getOriginalFilename()).orElse(defaultFileName);
                    }
                };
            }
            if (object instanceof File) {
                File file = (File) object;
                try (InputStream inputStream = new FileInputStream(file)) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] read = new byte[READ_LEN];
                    int len;
                    while ((len = inputStream.read(read, 0, READ_LEN)) > NOT_FOUND) {
                        outputStream.write(read, 0, len);
                    }
                    outputStream.close();
                    return new ByteArrayResource(outputStream.toByteArray()) {
                        @Override
                        public String getFilename() {
                            return file.getName();
                        }
                    };
                }
            }
        } catch (IOException e) {
            throw new FileException("文件解析失败", e);
        }
        throw new FileException("文件解析失败");
    }
}
