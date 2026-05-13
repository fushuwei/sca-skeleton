package io.github.fushuwei.scaskeleton.core.result;

import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author Fu Wei
 */
@Getter
public enum ResultCode {

    /**
     * 请求成功
     */
    SUCCESS(10000, "操作成功"),

    /**
     * 参数校验失败
     */
    VALIDATION_ERROR(40000, "请求参数不合法"),

    /**
     * 未认证（未登录或令牌无效）
     */
    UNAUTHORIZED(40100, "未认证或认证已失效"),

    /**
     * 无权限访问
     */
    FORBIDDEN(40300, "无权限访问"),

    /**
     * 业务警告（流程可继续）
     */
    WARNING(50000, "警告信息"),

    /**
     * 需要二次确认
     */
    CONFIRM(70000, "确认信息"),

    /**
     * 业务失败（兜底）
     */
    FAILURE(99999, "操作失败"),
    ;

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
