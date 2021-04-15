package com.wzb.rest.cache;

import com.wzb.rest.annation.RestRequestBody;
import com.wzb.rest.annation.RestRequestFile;
import com.wzb.rest.client.RestTemplateClient;
import com.wzb.rest.log.RestClientLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端解析内容缓存
 */
public final class ClientCacheFactory {

    private static final ClientCacheFactory factory = new ClientCacheFactory();

    private static final Map<String, MethodUrl> methodUrlMap = new ConcurrentHashMap<>();

    private static final Map<String, List<Class<?>>> responseClassMap = new ConcurrentHashMap<>();

    private static final Map<String, Boolean> urlContainsStaticParameterMap = new ConcurrentHashMap<>();

    private static final Map<String, Type> returnTypeMap = new ConcurrentHashMap<>();

    private static final Map<String, List<DynamicParameter>> dynamicParameterMap = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Object> restClientMap = new ConcurrentHashMap<>();

    private RequestAnnotationLink<?, ? extends Annotation> requestAnnotationLink;

    private ParameterAnnotationLink<?, ? extends Annotation>  parameterAnnotationLink;

    private RestTemplateClient restTemplateClient;

    private static final Map<String, EnumMap<ParameterType, List<ParameterSort>>> parameterSortTypeMap = new ConcurrentHashMap<>();

    private static final Map<String, Map<String, Integer>> pathParameterMap = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Object> nullResponseMap = new ConcurrentHashMap<>();

    private MethodInstance logbackMethod;

    private static final Map<Class<?>, MethodInstance> nullResponseMethodMap = new ConcurrentHashMap<>();

    private static final Map<Class<?>, Object> failBackResponseMap = new ConcurrentHashMap<>();

    private static final Map<Class<?>, MethodInstance> failBackResponseMethodMap = new ConcurrentHashMap<>();

    private static final Map<String, String> filePathMap = new ConcurrentHashMap<>();

    private Logger logger;

    /**
     * init
     */
    private ClientCacheFactory() {
        init();
    }

    /**
     * 获取实例
     *
     * @return {@link ClientCacheFactory}
     */
    public static ClientCacheFactory getInstance() {
        return factory;
    }

    /**
     * 已经加载 rest客户端代理
     *
     * @param clazz rest接口
     * @return boolean
     */
    public boolean hasLoadRestClientProxy(Class<?> clazz) {
        return restClientMap.containsKey(clazz) && restClientMap.get(clazz) != null;
    }

    /**
     * 获取rest客户端代理
     *
     * @param clazz rest接口
     * @return {@link Object}
     */
    public Object getRestClientProxy(Class<?> clazz) {
        return restClientMap.get(clazz);
    }

    /**
     * 缺省时装填
     *
     * @param clazz rest接口
     * @param proxy 代理
     */
    public void putIfAbsent(Class<?> clazz, Object proxy) {
        restClientMap.putIfAbsent(clazz, proxy);
    }

    /**
     * 获取rest模板
     *
     * @return {@link RestTemplateClient}
     */
    public RestTemplateClient getRestTemplateClient() {
        return restTemplateClient;
    }

    /**
     * 设置静止模板
     *
     * @param restTemplateClient rest模板
     */
    public void setRestTemplateClient(RestTemplateClient restTemplateClient) {
        this.restTemplateClient = restTemplateClient;
    }

    /**
     * 具有rest模板
     *
     * @return boolean
     */
    public boolean hasRestTemplateClient() {
        return Objects.nonNull(this.restTemplateClient);
    }

    /**
     * 生成方法key
     *
     * @param method 方法
     * @return {@link String}
     */
    public String generateMethodKey(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        return clazz.getName().concat("#").concat(method.getName());
    }

    /**
     * 缺省时装填
     *
     * @param methodKey 方法键
     * @param methodUrl 方法url
     */
    public void putIfAbsent(String methodKey, MethodUrl methodUrl) {
        methodUrlMap.putIfAbsent(methodKey, methodUrl);
    }

    /**
     * 获取方法url
     *
     * @param methodKey 方法键
     * @return {@link MethodUrl}
     */
    public MethodUrl getMethodUrl(String methodKey) {
        return methodUrlMap.get(methodKey);
    }

    /**
     * 具有方法url
     *
     * @param methodKey 方法键
     * @return boolean
     */
    public boolean hasMethodUrl(String methodKey) {
        return methodUrlMap.containsKey(methodKey);
    }

    /**
     * 具有get请求参数列表
     * @param methodKey 方法键
     * @return 结果
     */
    public boolean hasGetParameterSort(String methodKey) {
        return parameterSortTypeMap.containsKey(methodKey)
                && parameterSortTypeMap.get(methodKey).containsKey(ParameterType.PARAM)
                && !parameterSortTypeMap.get(methodKey).get(ParameterType.PARAM).isEmpty();
    }

    /**
     * 缺省时装填
     *
     * @param methodKey     方法键
     * @param parameterSort 参数排序
     */
    public void putIfAbsent(String methodKey, ParameterSort parameterSort) {
        if (parameterSort.getType() == ParameterType.PATH) {
            pathParameterMap.putIfAbsent(methodKey, new HashMap<>());
            pathParameterMap.get(methodKey).put(parameterSort.getName(), parameterSort.getIndex());
        } else {
            parameterSortTypeMap.putIfAbsent(methodKey, new EnumMap<>(ParameterType.class));
            parameterSortTypeMap.get(methodKey).putIfAbsent(parameterSort.getType(), new ArrayList<>());
            parameterSortTypeMap.get(methodKey).get(parameterSort.getType()).add(parameterSort);
        }
    }

    /**
     * 缺省时装填
     *
     * @param methodKey 方法键
     * @param path      路径
     */
    public void putIfAbsent(String methodKey, String path) {
        if (Objects.nonNull(path) && !path.isEmpty()) {
            filePathMap.putIfAbsent(methodKey, path);
        }
    }

    /**
     * 获取路径
     *
     * @param methodKey 方法键
     * @return {@link String}
     */
    public String getPath(String methodKey) {
        return filePathMap.get(methodKey);
    }

    /**
     * 有路径
     *
     * @param methodKey 方法键
     * @return boolean
     */
    public boolean hasPath(String methodKey) {
        return filePathMap.containsKey(methodKey);
    }

    /**
     * 缺省时装填
     *
     * @param clazz    类型
     * @param method   空响应方法
     * @param instance 实例
     */
    public void putNullResponseMethodIfAbsent(Class<?> clazz, Method method, Object instance) {
        nullResponseMethodMap.putIfAbsent(clazz, new MethodInstance(method, instance));
    }

    /**
     * 具有空响应方法
     *
     * @param clazz 类型
     * @return boolean
     */
    public boolean hasNullResponseMethod(Class<?> clazz) {
        return nullResponseMethodMap.containsKey(clazz);
    }

    /**
     * 获取空响应
     *
     * @param clazz   类型
     * @param nullMsg 空消息
     * @return {@link Object}
     */
    public Object getNullResponse(Class<?> clazz, CharSequence nullMsg) {
        MethodInstance methodInstance = nullResponseMethodMap.get(clazz);
        Object[] parameters = methodInstance.getParameters(nullMsg);
        try {
            return methodInstance.getMethod().invoke(methodInstance.getInstance(), parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.warn("执行空响应处理异常", e);
        }
        return null;
    }

    /**
     * 缺省时装填
     *
     * @param method   日志方法
     * @param instance 实例
     */
    public void setLogBackMethodIfAbsent(Method method, Object instance) {
        if (null == logbackMethod) {
            logbackMethod = new MethodInstance(method, instance);
        }
    }

    /**
     * 具有日志方法
     *
     * @return boolean
     */
    public boolean hasLogBackMethod() {
        return logbackMethod != null;
    }

    /**
     * 执行日志
     *
     * @param log 日志
     */
    public void invokeLogBackMethod(RestClientLog log) {
        if (null != logbackMethod) {
            try {
                logbackMethod.getMethod().invoke(logbackMethod.getInstance(), log);
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.warn("执行日志处理异常", e);
            }
        }
    }

    /**
     * 缺省时装填
     *
     * @param clazz            类型
     * @param failBackResponse 异常响应
     */
    public void putFailBackResponseIfAbsent(Class<?> clazz, Object failBackResponse) {
        failBackResponseMap.putIfAbsent(clazz, failBackResponse);
    }

    /**
     * 有降级处理响应
     *
     * @param clazz 类型
     * @return boolean
     */
    public boolean hasFailBackResponse(Class<?> clazz) {
        return failBackResponseMap.containsKey(clazz);
    }

    /**
     * 获取降级处理响应
     *
     * @param clazz 类型
     * @return {@link Object}
     */
    public Object getFailBackResponse(Class<?> clazz) {
        return failBackResponseMap.get(clazz);
    }

    /**
     * 缺省时装填
     *
     * @param clazz    类型
     * @param method   异常响应方法
     * @param instance 实例
     */
    public void putFailBackResponseMethodIfAbsent(Class<?> clazz, Method method, Object instance) {
        failBackResponseMethodMap.putIfAbsent(clazz, new MethodInstance(method, instance));
    }

    /**
     * 有异常响应方法
     *
     * @param clazz 类型
     * @return boolean
     */
    public boolean hasFailBackResponseMethod(Class<?> clazz) {
        return failBackResponseMethodMap.containsKey(clazz);
    }

    /**
     * 获取异常响应
     *
     * @param clazz     类型
     * @param throwable 异常
     * @param errorMsg  错误消息
     * @return {@link Object}
     */
    public Object getFailBackResponse(Class<?> clazz, Throwable throwable, CharSequence errorMsg) {
        MethodInstance methodInstance = failBackResponseMethodMap.get(clazz);
        Object[] parameters = methodInstance.getParameters(throwable, errorMsg);
        try {
            return methodInstance.getMethod().invoke(methodInstance.getInstance(), parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.warn("执行异常降级处理异常", e);
        }
        return null;
    }

    /**
     * 缺省时装填
     *
     * @param clazz        类型
     * @param nullResponse 空响应
     */
    public void putNullResponseIfAbsent(Class<?> clazz, Object nullResponse) {
        nullResponseMap.putIfAbsent(clazz, nullResponse);
    }

    /**
     * 获取空响应
     *
     * @param clazz 类型
     * @param <T>   泛型
     * @return {@link T}
     */
    @SuppressWarnings("unchecked")
    public <T> T getNullResponse(Class<? extends T> clazz) {
        Object nullResponse = nullResponseMap.get(clazz);
        if (null != nullResponse && nullResponse.getClass().isAssignableFrom(clazz)) {
            return (T) nullResponse;
        }
        return null;
    }

    /**
     * 具有空响应
     *
     * @param clazz 类型
     * @return boolean
     */
    public boolean hasNullResponse(Class<?> clazz) {
        return nullResponseMap.containsKey(clazz);
    }

    /**
     * 获取URL动态参数位置
     * @param methodKey 方法键
     * @param parameterName 参数名称
     * @return 位置索引
     */
    public Integer getPathParameterIndex(String methodKey, String parameterName) {
        if (pathParameterMap.containsKey(methodKey)) {
            return pathParameterMap.get(methodKey).get(parameterName);
        }
        return null;
    }

    /**
     * 按参数类型获取参数排序
     *
     * @param methodKey     方法键
     * @param parameterType 参数类型
     * @return {@link List<ParameterSort>}
     */
    public List<ParameterSort> getParameterSortByParameterType(String methodKey, ParameterType parameterType) {
        EnumMap<ParameterType, List<ParameterSort>> enumMap = parameterSortTypeMap.get(methodKey);
        if (Objects.isNull(enumMap) || enumMap.isEmpty() || !enumMap.containsKey(parameterType)) {
            return Collections.emptyList();
        }
        return enumMap.get(parameterType);
    }

    /**
     * 获取返回类型
     *
     * @param methodKey 方法键
     * @return Type
     */
    public Type getReturnType(String methodKey) {
        return returnTypeMap.get(methodKey);
    }

    /**
     * 具有返回类型
     *
     * @param methodKey 方法键
     * @return boolean
     */
    public boolean hasReturnType(String methodKey) {
        return returnTypeMap.containsKey(methodKey);
    }

    /**
     * 缺省时装填
     *
     * @param methodKey 方法键
     * @param type      方法返回类型
     */
    public void putIfAbsent(String methodKey, Type type) {
        returnTypeMap.putIfAbsent(methodKey, type);
    }

    /**
     * 具有方法响应类型
     *
     * @param methodKey 方法键
     * @return boolean
     */
    public boolean hasResponseClass(String methodKey) {
        return responseClassMap.containsKey(methodKey);
    }

    /**
     * 获取响应类型
     *
     * @param methodKey 方法键
     * @return List<Class < ?>>
     */
    public List<Class<?>> getResponseClass(String methodKey) {
        return responseClassMap.get(methodKey);
    }

    /**
     * 缺省时装填
     *
     * @param methodKey 方法键
     * @param classes   类型
     */
    public void putIfAbsent(String methodKey, List<Class<?>> classes) {
        responseClassMap.putIfAbsent(methodKey, classes);
    }

    /**
     * 具有动态参数
     *
     * @param methodKey 方法键
     * @return boolean
     */
    public boolean hasDynamicParameter(String methodKey) {
        return dynamicParameterMap.containsKey(methodKey);
    }

    /**
     * 获取动态参数
     *
     * @param methodKey 方法键
     * @return List<DynamicParameter>
     */
    public List<DynamicParameter> getDynamicParameter(String methodKey) {
        return dynamicParameterMap.get(methodKey);
    }

    /**
     * 缺省时装填
     *
     * @param methodKey         方法键
     * @param dynamicParameters 动态参数
     */
    public void putDynamicParameterIfAbsent(String methodKey, List<DynamicParameter> dynamicParameters) {
        dynamicParameterMap.putIfAbsent(methodKey, dynamicParameters);
    }

    /**
     * 获取请求注解链
     *
     * @return {@link RequestAnnotationLink}
     */
    public RequestAnnotationLink getRequestAnnotationLink() {
        return this.requestAnnotationLink;
    }

    /**
     * 获取参数注解链
     *
     * @return {@link ParameterAnnotationLink}
     */
    public ParameterAnnotationLink getParameterAnnotationLink() {
        return this.parameterAnnotationLink;
    }

    /**
     * 设置方法含有静态参数标识
     * @param methodKey 方法key
     * @param urlContainStaticParameter 标识
     */
    public void putIfAbsent(String methodKey, boolean urlContainStaticParameter) {
        urlContainsStaticParameterMap.putIfAbsent(methodKey, urlContainStaticParameter);
    }

    /**
     * 方法含有静态参数标识
     * @param methodKey 方法key
     * @return 结果
     */
    public boolean methodUrlContainStaticParameter(String methodKey) {
        return urlContainsStaticParameterMap.get(methodKey);
    }

    /**
     * 初始化
     */
    private void init() {
        requestAnnotationLink = RequestAnnotationLink.build(
                RequestAnnotation.builder(PostMapping.class)
                        .valueFunction(PostMapping::value)
                        .pathFunction(PostMapping::path)
                        .producesFunction(PostMapping::produces)
                        .defaultHttpMethod(HttpMethod.POST)
                        .build(),
                RequestAnnotation.builder(GetMapping.class)
                        .valueFunction(GetMapping::value)
                        .pathFunction(GetMapping::path)
                        .producesFunction(GetMapping::produces)
                        .defaultHttpMethod(HttpMethod.GET)
                        .build(),
                RequestAnnotation.builder(RequestMapping.class)
                        .valueFunction(RequestMapping::value)
                        .pathFunction(RequestMapping::path)
                        .producesFunction(RequestMapping::produces)
                        .httpMethodFunction(RequestMapping::method)
                        .defaultHttpMethod(HttpMethod.GET)
                        .build(),
                RequestAnnotation.builder(DeleteMapping.class)
                        .valueFunction(DeleteMapping::value)
                        .pathFunction(DeleteMapping::path)
                        .producesFunction(DeleteMapping::produces)
                        .defaultHttpMethod(HttpMethod.DELETE)
                        .build(),
                RequestAnnotation.builder(PutMapping.class)
                        .valueFunction(PutMapping::value)
                        .pathFunction(PutMapping::path)
                        .producesFunction(PutMapping::produces)
                        .defaultHttpMethod(HttpMethod.PUT)
                        .build()
        );
        parameterAnnotationLink = ParameterAnnotationLink.build(
                new ParameterAnnotation<>(RequestParam.class, ParameterType.PARAM, RequestParam::value),
                new ParameterAnnotation<>(RequestHeader.class, ParameterType.HEADER, RequestHeader::value),
                new ParameterAnnotation<>(RequestBody.class, ParameterType.BODY),
                new ParameterAnnotation<>(PathVariable.class, ParameterType.PATH, PathVariable::value),
                new ParameterAnnotation<>(RestRequestBody.class, ParameterType.REST),
                new ParameterAnnotation<>(RestRequestFile.class, ParameterType.FILE, RestRequestFile::name, RestRequestFile::path)
        );

        logger = LoggerFactory.getLogger(getClass());
    }

    /**
     * 类实例
     */
    static class MethodInstance {

        private static final int NOT_FOUND = -1;

        private Method method;

        private Object instance;

        private int throwableIndex = NOT_FOUND;

        private int msgIndex = NOT_FOUND;

        /**
         * 类实例
         *
         * @param method   方法
         * @param instance 实例
         */
        MethodInstance(Method method, Object instance) {
            this.method = method;
            this.instance = instance;
            init();
        }

        /**
         * 初始化
         */
        private void init() {
            Class<?>[] parameterClass = method.getParameterTypes();
            for (int i = 0; i < parameterClass.length; i++) {
                if (throwableIndex < 0 && Throwable.class.isAssignableFrom(parameterClass[i])) {
                    throwableIndex = i;
                }
                if (msgIndex < 0 && CharSequence.class.isAssignableFrom(parameterClass[i])) {
                    msgIndex = i;
                }
            }
        }

        /**
         * 获取方法
         *
         * @return {@link Method}
         */
        public Method getMethod() {
            return method;
        }

        /**
         * 获取实例
         *
         * @return {@link Object}
         */
        public Object getInstance() {
            return instance;
        }

        /**
         * 获取参数
         *
         * @param parameter 参数
         * @return {@link Object[]}
         */
        public Object[] getParameters(CharSequence parameter) {
            Object[] parameters = new Object[method.getParameterCount()];
            if (msgIndex > NOT_FOUND) {
                parameters[msgIndex] = parameter;
            }
            return parameters;
        }

        /**
         * 获取参数
         *
         * @param throwable 异常参数
         * @param errorMsg  异常消息参数
         * @return {@link Object[]}
         */
        public Object[] getParameters(Throwable throwable, CharSequence errorMsg) {
            Object[] parameters = new Object[method.getParameterCount()];
            if (throwableIndex > NOT_FOUND) {
                parameters[throwableIndex] = throwable;
            }
            if (msgIndex > NOT_FOUND) {
                parameters[msgIndex] = errorMsg;
            }
            return parameters;
        }
    }
}
