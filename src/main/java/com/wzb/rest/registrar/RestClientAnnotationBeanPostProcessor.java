package com.wzb.rest.registrar;


import com.wzb.rest.annation.RestClient;
import com.wzb.rest.proxy.RestClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * 注解@RestClient bean加载器
 */
public class RestClientAnnotationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor,
        EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<String> packagesToScan;

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    private Map<String, Object> restAttributes;

    /**
     * RestClientAnnotationBeanPostProcessor
     * @param packagesToScan packagesToScan
     */
    public RestClientAnnotationBeanPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        Set<String> resolvedPackagesToScan = resolvePackagesToScan(packagesToScan);
        // 加入自定义扫描路径
        extractedScanPackages(resolvedPackagesToScan);

        if (!CollectionUtils.isEmpty(resolvedPackagesToScan)) {
            registerRestClientBeans(resolvedPackagesToScan, registry);
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("packagesToScan is empty , RestClient registry will be ignored!");
            }
        }

    }

    /**
     * 扩展扫描路径
     * @param resolvedPackagesToScan resolvedPackagesToScan
     */
    private void extractedScanPackages(Set<String> resolvedPackagesToScan) {
        // 自定义扫描路径
        String[] restScanPackages = (String[]) restAttributes.get("scanPackages");
        if (restScanPackages.length > 0) {
            for (String packageToScan : restScanPackages) {
                if (StringUtils.hasText(packageToScan)) {
                    String resolvedPackageToScan = environment.resolvePlaceholders(packageToScan.trim());
                    resolvedPackagesToScan.add(resolvedPackageToScan);
                }
            }
        }
    }

    /**
     * 注册 @RestClient bean
     *
     * @param packagesToScan packagesToScan
     * @param registry       registry
     */
    private void registerRestClientBeans(Set<String> packagesToScan, BeanDefinitionRegistry registry) {
        //@RestClient注解扫描器
        RestClientClassPathBeanDefinitionScanner scanner =
                new RestClientClassPathBeanDefinitionScanner(false, environment);
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestClient.class));

        for (String packageToScan : packagesToScan) {

            // Registers @RestClient Bean first
            // Finds all BeanAnnotationMetadata of @RestClient.
            Set<AnnotationMetadata> beanAnnotationMetadata =
                    findRestClientBeanAnnotationMetadata(scanner, packageToScan);

            if (!CollectionUtils.isEmpty(beanAnnotationMetadata)) {

                for (AnnotationMetadata annotationMetadata : beanAnnotationMetadata) {
                    //注册
                    registerRestClientBean(annotationMetadata, registry);
                }

                if (logger.isInfoEnabled()) {
                    logger.info(beanAnnotationMetadata.size() + " annotated @RestClient Components { "
                            + beanAnnotationMetadata
                            + " } were scanned under package[" + packageToScan + "]");
                }

            } else {

                if (logger.isWarnEnabled()) {
                    logger.warn("No Spring Bean annotating @RestClient was found under package["
                            + packageToScan + "]");
                }

            }

        }

    }

    /**
     * 找到@RestClient注解 bean对象注释元数据
     *
     * @param scanner       扫描仪
     * @param packageToScan 要扫描的包
     * @return {@link Set<AnnotationMetadata>}
     */
    private Set<AnnotationMetadata> findRestClientBeanAnnotationMetadata(
            ClassPathScanningCandidateComponentProvider scanner,
            String packageToScan) {

        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(packageToScan);

        Set<AnnotationMetadata> beanAnnotationMetadata = new LinkedHashSet<>(beanDefinitions.size());

        for (BeanDefinition beanDefinition : beanDefinitions) {

            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                // verify annotated class is an interface
                AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDefinition;
                AnnotationMetadata annotationMetadata = annotatedBeanDefinition.getMetadata();
                Assert.isTrue(annotationMetadata.isInterface(),
                        "@RestClient can only be specified on an interface");

                beanAnnotationMetadata.add(annotationMetadata);
            }

        }
        return beanAnnotationMetadata;
    }

    /**
     * 注册@RestClient bean对象
     *
     * @param annotationMetadata 注释元数据
     * @param registry           bean注册器
     */
    private void registerRestClientBean(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {

        String className = annotationMetadata.getClassName();

        //加载接口类
        Class<?> interfaceClass = resolveClassName(className, classLoader);

        //获取@RestClient注解属性
        Map<String, Object> attributes = annotationMetadata
                .getAnnotationAttributes(RestClient.class.getCanonicalName());

        //创建bean
        AbstractBeanDefinition restClientBeanDefinition = buildRestClientBeanDefinition(attributes, interfaceClass);

        //bean持有信息
        BeanDefinitionHolder holder = new BeanDefinitionHolder(restClientBeanDefinition, className,
                new String[]{UUID.randomUUID() + "@RestClient"});

        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    /**
     * 解析要扫描的包
     *
     * @param packagesToScan 要扫描的包
     * @return {@link Set<String>}
     */
    private Set<String> resolvePackagesToScan(Set<String> packagesToScan) {
        Set<String> resolvedPackagesToScan = new LinkedHashSet<>(packagesToScan.size());
        for (String packageToScan : packagesToScan) {
            if (StringUtils.hasText(packageToScan)) {
                String resolvedPackageToScan = environment.resolvePlaceholders(packageToScan.trim());
                resolvedPackagesToScan.add(resolvedPackageToScan);
            }
        }
        return resolvedPackagesToScan;
    }

    /**
     * 建造 restClient bean对象定义
     *
     * @param attributes          属性
     * @param restClientInterface rest客户端接口
     * @return {@link AbstractBeanDefinition}
     */
    private AbstractBeanDefinition buildRestClientBeanDefinition(Map<String, Object> attributes,
                                                                 Class<?> restClientInterface) {
        //构建代理类
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RestClientProxy.class);

        builder.addPropertyValue("attributes", attributes);
        builder.addPropertyValue("restClientInterface", restClientInterface);
        builder.addPropertyValue("environment", environment);
        builder.addPropertyValue("classLoader", classLoader);
        builder.addPropertyValue("restTemplateName", restAttributes.get("value"));
        builder.addPropertyValue("mode", restAttributes.get("mode"));
        builder.addPropertyValue("configureClass", restAttributes.get("configureClass"));

        AbstractBeanDefinition definition = builder.getBeanDefinition();

        //这里采用的是byType方式注入，类似的还有byName等
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        return definition;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 设置模板
     *
     * @param restAttributes rest模板参数
     */
    public void setRestAttributes(Map<String, Object> restAttributes) {
        this.restAttributes = restAttributes;
    }
}
