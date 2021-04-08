package com.wzb.rest.registrar;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;

/**
 * 注解@RestClient扫描器
 */
public class RestClientClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider {

    /**
     * RestClientClassPathBeanDefinitionScanner
     * @param useDefaultFilters useDefaultFilters
     * @param environment environment
     */
    public RestClientClassPathBeanDefinitionScanner(boolean useDefaultFilters, Environment environment) {
        super(useDefaultFilters, environment);
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        boolean isCandidate = false;
        if (beanDefinition.getMetadata().isIndependent()) {
            //扫描加了注解的类
            if (!beanDefinition.getMetadata().isAnnotation()) {
                isCandidate = true;
            }
        }
        return isCandidate;
    }
}
