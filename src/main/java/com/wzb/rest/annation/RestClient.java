package com.wzb.rest.annation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * restClient
 * 只能加在interface上
 * 配合requestMapping & postMapping & getMapping & deleteMapping & putMapping
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestClient {

    /**
     * 服务
     * @return 服务
     */
    String value() default "";

    /**
     * 路由
     * @return 路由
     */
    String route() default "";

    /**
     * 客户端名称
     * @return 客户端名称
     */
    String name() default "";


}
