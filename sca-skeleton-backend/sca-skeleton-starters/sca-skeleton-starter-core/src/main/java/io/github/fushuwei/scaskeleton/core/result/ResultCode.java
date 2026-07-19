package io.github.fushuwei.scaskeleton.core.result;

import lombok.Getter;

/**
 * 统一响应状态码枚举
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
     * 业务警告
     */
    WARNING(30000, "警告信息"),

    /**
     * 参数校验失败
     */
    VALIDATION_ERROR(40000, "请求参数不合法"),

    /**
     * 未认证
     */
    UNAUTHORIZED(40100, "登录已过期，请重新登录"),

    /**
     * 无权限访问
     */
    FORBIDDEN(40300, "权限不足，无法访问该功能"),

    /**
     * 资源不存在
     */
    NOT_FOUND(40400, "请求的资源不存在"),

    /**
     * 请求方法不允许
     */
    METHOD_NOT_ALLOWED(40500, "请求方法不允许"),

    /**
     * 资源已存在
     */
    ALREADY_EXISTS(40900, "资源已存在"),

    /**
     * 乐观锁版本冲突：数据已被其他用户修改，需刷新后重试
     */
    VERSION_CONFLICT(40901, "数据已被他人修改，请刷新后重试"),

    /**
     * 请求过于频繁
     */
    TOO_MANY_REQUESTS(42900, "请求过于频繁，请稍后重试"),

    /**
     * 服务器内部错误
     */
    INTERNAL_SERVER_ERROR(50000, "系统繁忙，请稍后重试"),

    /**
     * 网关异常
     */
    BAD_GATEWAY(50200, "服务响应异常，请稍后重试"),

    /**
     * 服务不可用
     */
    SERVICE_UNAVAILABLE(50300, "服务暂时不可用，请稍后重试"),

    /**
     * 网关超时
     */
    GATEWAY_TIMEOUT(50400, "服务响应超时，请稍后重试"),

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
