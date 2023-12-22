package com.kyle.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DBRouter {
    /**
     * 分库分表字段注解
     * @return
     */
    String key() default "";
}
