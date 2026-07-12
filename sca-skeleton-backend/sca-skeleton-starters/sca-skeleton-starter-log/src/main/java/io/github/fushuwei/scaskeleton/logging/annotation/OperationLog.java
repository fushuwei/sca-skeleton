package io.github.fushuwei.scaskeleton.logging.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 层方法上，AOP 切面将自动记录操作人、操作模块、操作类型、请求参数、响应结果、执行耗时、操作 IP 等信息
 *
 * @author Fu Wei
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作所属模块名称，如"用户管理"、"权限管理"
     */
    String module();

    /**
     * 操作动作描述，如"新增用户"、"修改密码"、"导出数据"
     */
    String action();

    /**
     * 是否记录请求参数，默认记录涉及敏感数据（密码、Token）的接口可设为 false
     */
    boolean logArgs() default true;

    /**
     * 是否记录响应结果，默认记录响应体较大时可设为 false 以减少存储开销
     */
    boolean logResult() default true;
}
