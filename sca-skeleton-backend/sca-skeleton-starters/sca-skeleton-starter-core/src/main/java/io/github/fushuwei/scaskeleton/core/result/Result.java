package io.github.fushuwei.scaskeleton.core.result;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.github.fushuwei.scaskeleton.core.trace.TraceContext;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * 响应结果
 *
 * @param <T> 响应数据类型
 * @author Fu Wei
 */
@Data
@Accessors(fluent = true, chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "code",
    "message",
    "data",
    "type",
    "confirmToken",
    "traceId",
    "timestamp"
})
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    @JsonProperty("code")
    private Integer code;

    /**
     * 响应消息
     */
    @JsonProperty("message")
    private String message;

    /**
     * 响应数据
     */
    @JsonProperty("data")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private T data;

    /**
     * 响应类型：SUCCESS, WARNING, CONFIRM, FAILURE
     */
    @JsonProperty("type")
    private ResultType type;

    /**
     * 二次确认时的令牌，前端需要在确认时回传，防止重放攻击和确保操作的一致性
     */
    @JsonProperty("confirmToken")
    private String confirmToken;

    /**
     * 链路追踪 ID
     */
    @JsonProperty("traceId")
    private String traceId;

    /**
     * 时间戳
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * 构造方法
     */
    private Result() {
        // 从 TraceContext 中提取 traceId，保证链路追踪在统一响应里可见
        this.traceId = TraceContext.get();
        // 统一在构造时写入时间戳，避免调用方重复处理
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构造方法
     */
    private Result(Integer code, String message, T data, ResultType type) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
        this.type = type;
    }

    /**
     * 构造方法
     */
    private Result(Integer code, String message, T data, ResultType type, String confirmToken) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
        this.type = type;
        this.confirmToken = confirmToken;
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> ok() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null, ResultType.SUCCESS);
    }

    /**
     * 成功响应（自定义消息）
     */
    public static <T> Result<T> ok(String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null, ResultType.SUCCESS);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, ResultType.SUCCESS);
    }

    /**
     * 成功响应（自定义消息和数据）
     */
    public static <T> Result<T> ok(T data, String message) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data, ResultType.SUCCESS);
    }

    /**
     * 失败响应（自定义消息）
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(ResultCode.FAILURE.getCode(), message, null, ResultType.FAILURE);
    }

    /**
     * 失败响应（标准状态码）
     */
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null, ResultType.FAILURE);
    }

    /**
     * 失败响应（标准状态码，自定义消息）
     */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null, ResultType.FAILURE);
    }

    /**
     * 失败响应（自定义消息和数据）
     */
    public static <T> Result<T> fail(T data, String message) {
        return new Result<>(ResultCode.FAILURE.getCode(), message, data, ResultType.FAILURE);
    }

    /**
     * 警告响应（自定义消息）
     */
    public static <T> Result<T> warn(String message) {
        return new Result<>(ResultCode.WARNING.getCode(), message, null, ResultType.WARNING);
    }

    /**
     * 警告响应（标准状态码）
     */
    public static <T> Result<T> warn(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null, ResultType.WARNING);
    }

    /**
     * 警告响应（自定义消息和数据）
     */
    public static <T> Result<T> warn(T data, String message) {
        return new Result<>(ResultCode.WARNING.getCode(), message, data, ResultType.WARNING);
    }

    /**
     * 需要二次确认的响应
     */
    public static <T> Result<T> confirm(String message, String confirmToken) {
        return new Result<>(ResultCode.CONFIRM.getCode(), message, null, ResultType.CONFIRM, confirmToken);
    }

    /**
     * 需要二次确认的响应（带提示数据）
     */
    public static <T> Result<T> confirm(T data, String message, String confirmToken) {
        return new Result<>(ResultCode.CONFIRM.getCode(), message, data, ResultType.CONFIRM, confirmToken);
    }

    /**
     * 标准状态码响应
     */
    public static <T> Result<T> of(ResultCode resultCode, ResultType type) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null, type);
    }

    /**
     * 标准状态码响应（带数据）
     */
    public static <T> Result<T> of(ResultCode resultCode, T data, ResultType type) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), data, type);
    }

    /**
     * 自定义状态码响应
     */
    public static <T> Result<T> of(Integer code, String message, ResultType type) {
        return new Result<>(code, message, null, type);
    }

    /**
     * 自定义状态码响应
     */
    public static <T> Result<T> of(Integer code, String message, T data, ResultType type) {
        return new Result<>(code, message, data, type);
    }

    /**
     * 自定义状态码响应
     */
    public static <T> Result<T> of(Integer code, String message, T data, ResultType type, String confirmToken) {
        return new Result<>(code, message, data, type, confirmToken);
    }

    /**
     * 判断请求是否成功
     */
    @JsonIgnore
    public Boolean isSuccess() {
        return Objects.equals(ResultType.SUCCESS, this.type) && Objects.equals(ResultCode.SUCCESS.getCode(), this.code);
    }
}
