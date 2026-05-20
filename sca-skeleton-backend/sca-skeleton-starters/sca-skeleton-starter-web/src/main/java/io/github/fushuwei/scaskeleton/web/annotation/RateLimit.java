package io.github.fushuwei.scaskeleton.web.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解。
 * <p>
 * 基于 Redisson RRateLimiter 实现令牌桶限流，标注在 Controller 方法上，
 * AOP 切面自动拦截并判断当前请求是否超过速率限制。
 * <p>
 * 示例：
 * <pre>{@code
 * // 每秒最多 10 次请求，超限返回 TOO_MANY_REQUESTS 错误
 * @RateLimit(key = "sms:send", rate = 10, rateInterval = 1)
 * @PostMapping("/sms/send")
 * public Result<Void> sendSms(...) { ... }
 * }</pre>
 *
 * @author Fu Wei
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流 Key 前缀，建议格式：{业务域}:{动作}，如 {@code sms:send}、{@code order:create}。
     * AOP 切面将自动补充维度后缀（全局/用户/IP）。
     */
    String key();

    /**
     * 速率间隔内允许的最大请求数。
     */
    long rate() default 10;

    /**
     * 速率统计时间窗口大小，配合 {@link #rateIntervalUnit()} 使用。
     */
    long rateInterval() default 1;

    /**
     * 速率统计时间窗口单位，默认秒。
     */
    TimeUnit rateIntervalUnit() default TimeUnit.SECONDS;

    /**
     * 限流维度：{@link RateLimitType#GLOBAL} 全局共享同一计数器；
     * {@link RateLimitType#USER} 按当前用户 ID 隔离；
     * {@link RateLimitType#IP} 按请求 IP 隔离。
     */
    RateLimitType type() default RateLimitType.GLOBAL;

    /**
     * 限流触发时的提示信息，默认使用通用错误描述。
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 限流维度枚举。
     */
    enum RateLimitType {
        /** 全局维度：所有用户共享一个限流计数器 */
        GLOBAL,
        /** 用户维度：每个用户独立计数，需要 CurrentUserProvider 支持 */
        USER,
        /** IP 维度：每个来源 IP 独立计数 */
        IP
    }
}
