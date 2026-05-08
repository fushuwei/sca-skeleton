package io.github.fushuwei.sca.starter.web.response;

import io.github.fushuwei.sca.starter.core.exception.ErrorCode;

/**
 * 统一 API 响应体。
 *
 * @author Fu Wei
 */
public record ApiResponse<T>(String code, String message, T data) {

    // 构造成功响应。
    public static <T> ApiResponse<T> success(T data) {
        // 使用统一成功码构建响应。
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    // 构造失败响应。
    public static <T> ApiResponse<T> failure(String code, String message) {
        // 使用入参错误信息构建失败响应。
        return new ApiResponse<>(code, message, null);
    }
}
