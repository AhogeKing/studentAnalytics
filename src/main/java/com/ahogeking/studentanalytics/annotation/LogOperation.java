package com.ahogeking.studentanalytics.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /**
     * 模块名，例如：
     * user / student / performance / model / prediction / warning
     */
    String module();

    /**
     * 操作类型
     * CREATE / UPDATE / DELETE / UPSERT / TRAIN / PREDICT
     */
    String type();

    /**
     * 操作对象类型
     * USER / STUDENT / PERFORMANCE / MODEL_VERSION
     */
    String targetType() default "";

    /**
     * SpEL 表达式
     * <p>
     * 示例：
     * #id
     * #studentNo
     * #request.studentNo
     * #request.username
     */
    String targetId() default "";

    /**
     * 业务标识
     * <p>
     * e.g.
     * #studentNo
     * #request.username
     * #request.studentNo
     */
    String businessKey() default "";

    /**
     * 是否记录请求参数和请求体
     */
    boolean recordRequest() default true;
}
