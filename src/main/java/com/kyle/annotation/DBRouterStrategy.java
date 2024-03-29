package com.kyle.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DBRouterStrategy {
    //分表策略
    boolean spiltTable() default false;
}
