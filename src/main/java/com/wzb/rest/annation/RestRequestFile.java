package com.wzb.rest.annation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RestRequestFile
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RestRequestFile {

    /**
     * 路径
     *
     * @return {@link String}
     */
    String path() default "";

    /**
     * 名称
     *
     * @return {@link String}
     */
    String name() default "";
}
