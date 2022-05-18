package com.wzb.rest.proxy;

import com.wzb.rest.cache.ClientCacheFactory;
import com.wzb.rest.client.RestTemplateClient;
import com.wzb.rest.config.MessageConvertConfigure;
import com.wzb.rest.instance.InstanceFactory;
import com.wzb.rest.resolver.ConfigureClassResolver;
import com.wzb.rest.resolver.MethodResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * restClient代理
 * 1.解析请求及响应
 * 2.日志aop
 */
public class RestClientProxy implements FactoryBean<Object>, InitializingBean, ApplicationContextAware {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * java 动态代理
     */
    public static final int MODE_PROXY = 1;

    /**
     * javassist 动态代理
     */
    public static final int MODE_JAVASSIST = 2;

    private static final String PROPERTY_SYMBOL_START = "${";

    private static final String PROPERTY_SYMBOL_END = "}";

    private static final String HTTPS = "https://";

    private static final String HTTP = "http://";

    private static final int FIND_INDEX = -1;

    private ClientCacheFactory factory = ClientCacheFactory.getInstance();

    private MethodResolver methodResolver = MethodResolver.getInstance();

    private ConfigureClassResolver configureClassResolver = ConfigureClassResolver.getInstance();

    /**
     * 注解@RestClient上的属性
     */
    private Map<String, Object> attributes;

    private Class<?> restClientInterface;

    private String restTemplateName;

    private int mode;

    private Object proxy;

    private Environment environment;

    private ClassLoader classLoader;

    private ApplicationContext applicationContext;

    private String url;

    private Class<?>[] configureClass;

    /**
     * 构造方法
     */
    public RestClientProxy() {

    }

    /**
     * 设置被代理的接口
     *
     * @param restClientInterface restClientInterface
     */
    public void setRestClientInterface(Class<?> restClientInterface) {
        this.restClientInterface = restClientInterface;
    }

    @Override
    public Object getObject() throws Exception {
        return getProxy();
    }

    /**
     * 获取代理
     *
     * @return {@link Object}
     */
    private Object getProxy() {
        if (!factory.hasLoadRestClientProxy(restClientInterface)) {
            if (logger.isDebugEnabled()) {
                logger.debug(">>>>>>>>>>>>>>rest-client-proxy load proxy of {}", restClientInterface.getName());
            }
            //解析路由
            resolverRoute();
            //解析服务
            resolverValue();
            //解析接口方法
            methodResolver.resolverMethod(restClientInterface, this.url);
            //创建代理
            Object restProxy;
            if (mode == MODE_JAVASSIST) {
                restProxy = InstanceFactory.createInstance(this.restClientInterface);
            } else {
                restProxy = InstanceFactory.createProxy(this.restClientInterface);
            }
            factory.putIfAbsent(restClientInterface, restProxy);
        }
        //设置rest请求模板
        if (!factory.hasRestTemplateClient()) {
            RestTemplate restTemplate = null;
            //使用预配置的Rest模板
            if (!this.restTemplateName.isEmpty()) {
                restTemplate = (RestTemplate) applicationContext.getBean(this.restTemplateName);
            }
            MessageConvertConfigure defaultConverter = applicationContext.getBean(MessageConvertConfigure.class);
            List<HttpMessageConverter<?>> requestConverters = defaultConverter.getRequestHttpMessageConverter();
            List<HttpMessageConverter<?>> responseConverters = defaultConverter.getResponseHttpMessageConverter();
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
            //解析配置
            configureClassResolver.resolver(this.configureClass, requestConverters,
                    responseConverters, interceptors, this.factory, this.applicationContext);
            RestTemplateClient restTemplateClient = new RestTemplateClient(restTemplate,
                    requestConverters, responseConverters, interceptors);
            factory.setRestTemplateClient(restTemplateClient);
        }
        this.proxy = factory.getRestClientProxy(restClientInterface);
        return this.proxy;
    }

    @Override
    public Class<?> getObjectType() {
        return this.restClientInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * 设置类加载器
     *
     * @param classLoader classLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 设置代理模式
     * @param mode 模式
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }

    /**
     * 设置环境变量
     *
     * @param environment environment
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 设置注解@RestClient属性
     *
     * @param attributes attributes
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /**
     * 设置模板
     *
     * @param restTemplateName rest模板名称
     */
    public void setRestTemplateName(String restTemplateName) {
        this.restTemplateName = restTemplateName;
    }

    /**
     * 设置配置类
     *
     * @param configureClass 配置类
     */
    public void setConfigureClass(Class<?>[] configureClass) {
        this.configureClass = configureClass;
    }

    /**
     * 解析路由
     */
    private void resolverRoute() {
        String route = (String) this.attributes.get("route");
        //从环境变量里取
        if (route.startsWith(PROPERTY_SYMBOL_START) && route.endsWith(PROPERTY_SYMBOL_END)) {
            String propertyName = route.substring(PROPERTY_SYMBOL_START.length(),
                    route.length() - PROPERTY_SYMBOL_END.length());
            int defaultValueIndex = propertyName.indexOf(':');
            String defaultValue = "";
            if (defaultValueIndex > FIND_INDEX) {
                defaultValue = propertyName.substring(defaultValueIndex + 1);
                propertyName = propertyName.substring(0, defaultValueIndex);
            }
            route = environment.getProperty(propertyName, defaultValue);
            Assert.hasText(route, String.format("not found property of %s", propertyName));
            route = route.replace("\\", "/");
        } else if (!route.isEmpty() && !route.startsWith(HTTPS) && !route.startsWith(HTTP)) {
            //直接取
            route = route.replace("\\", "/");
            if (route.startsWith("/")) {
                route = route.substring(1);
            }
            route = HTTP.concat(route);
        }
        this.url = route;
    }

    /**
     * 解析服务
     */
    private void resolverValue() {
        StringBuilder urlBuilder = new StringBuilder();
        String path = ((String) this.attributes.get("value")).replace("\\", "/");
        if (this.url.isEmpty()) {
            if (!path.startsWith(HTTPS) && !path.startsWith(HTTP)) {
                urlBuilder.append(HTTP);
            }
        } else {
            //地址设置冲突
            if (path.startsWith(HTTPS) || path.startsWith(HTTP)) {
                throw new IllegalArgumentException(
                        String.format("url resolver fail [route:%s] [value:%s] @RestClient",
                                this.attributes.get("route"),
                                this.attributes.get("value")));
            }
            urlBuilder.append(this.url);
        }
        if (urlBuilder.length() > 0 && urlBuilder.charAt(urlBuilder.length() - 1) != '/') {
            urlBuilder.append("/");
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        urlBuilder.append(path);
        this.url = urlBuilder.toString();
    }
}
