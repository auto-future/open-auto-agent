package cn.unicom.soc.servers.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description
 * @Author chenss
 * @CreateTime 2025-04-02 13:56:43
 * @ModifyTime
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Resource {
    String uri() default "";
    String name() default "";
    String description() default "";
    String mimeType() default "";
    String[] audience() default {};
    double priority() default 1.0;
}
