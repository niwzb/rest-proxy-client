package com.wzb.rest.registrar;

import com.wzb.rest.annation.EnableRestClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * 扫描加载注解@RestClient类
 */
public class RestClientScanRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        //rest注解属性
        Map<String, Object> restAttributes = metadata.getAnnotationAttributes(EnableRestClient.class.getName());
        //扫描启动类下的子级包
        String classPackage = ClassUtils.getPackageName(metadata.getClassName());
        Set<String> packagesToScan = Collections.singleton(classPackage);
        BeanDefinitionBuilder builder = rootBeanDefinition(RestClientAnnotationBeanPostProcessor.class);
        builder.addConstructorArgValue(packagesToScan);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        builder.addPropertyValue("restAttributes", restAttributes);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }
}
