package com.wzb.rest.annation;


import com.wzb.rest.config.MessageConvertConfigure;
import com.wzb.rest.registrar.RestClientScanRegistrar;
import com.wzb.rest.proxy.RestClientProxy;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用restClient
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Import({RestClientScanRegistrar.class, MessageConvertConfigure.class})
public @interface EnableRestClient {

    /**
     * springContext 里的rest请求模板
     * @return rest
     */
    String value() default "restTemplate";

    /**
     * 代理方式 <br>
     *     可选项：{@link RestClientProxy#MODE_PROXY},
     *     {@link RestClientProxy#MODE_JAVASSIST}
     * @return mode
     */
    int mode() default RestClientProxy.MODE_PROXY;

    /**
     * 配置类
     *
     * @return {@link Class<?>[]}
     */
    Class<?>[] configureClass() default {};
}
