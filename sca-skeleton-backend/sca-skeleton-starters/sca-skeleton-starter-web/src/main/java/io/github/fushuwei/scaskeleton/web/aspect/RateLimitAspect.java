package io.github.fushuwei.scaskeleton.web.aspect;

import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.web.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.jspecify.annotations.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 限流切面。
 * <p>
 * 拦截标注了 {@link RateLimit} 的方法，基于 Redisson RRateLimiter 执行令牌桶限流。
 * 支持三种限流维度：全局、用户 ID、请求 IP。超限时抛出 {@link BusinessException}。
 *
 * @author Fu Wei
 */
@Slf4j
@Aspect
public class RateLimitAspect {

    // Redisson 客户端，用于获取分布式令牌桶 RRateLimiter
    private final RedissonClient redissonClient;

    // 可选注入：按用户维度限流时需要获取当前用户 ID
    @Nullable
    private final CurrentUserProvider currentUserProvider;

    /** Redis Key 前缀，避免与其他业务 Key 冲突 */
    private static final String KEY_PREFIX = "rate_limit:";

    public RateLimitAspect(RedissonClient redissonClient,
                           @Nullable CurrentUserProvider currentUserProvider) {
        this.redissonClient = redissonClient;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 环绕通知：在目标方法执行前尝试获取令牌，失败则抛出限流异常。
     *
     * @param joinPoint  切入点
     * @param annotation 方法上的 @RateLimit 注解
     * @return 原方法返回值
     */
    @Around("@annotation(annotation)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit annotation) throws Throwable {
        // 根据限流维度构建唯一 Redis Key
        String rateLimitKey = buildKey(annotation);

        // 获取或初始化 Redisson 分布式令牌桶
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimitKey);

        // trySetRate 是幂等的，令牌桶已存在时不重复初始化
        rateLimiter.trySetRate(
                RateType.OVERALL,
                annotation.rate(),
                annotation.rateInterval(),
                toRedissonUnit(annotation.rateIntervalUnit())
        );

        // 尝试立即获取一个令牌，无令牌可用时立即返回 false（不阻塞）
        if (!rateLimiter.tryAcquire()) {
            log.warn("[RateLimit] triggered. key={} rate={}/{}{}", rateLimitKey,
                    annotation.rate(), annotation.rateInterval(),
                    annotation.rateIntervalUnit().name().toLowerCase());
            // 超限时抛出业务异常，由全局异常处理器统一响应
            throw BusinessException.of(ResultCode.TOO_MANY_REQUESTS, annotation.message());
        }

        // 令牌获取成功，执行原方法
        return joinPoint.proceed();
    }

    /**
     * 根据限流维度构建 Redis Key。
     * GLOBAL → rate_limit:{key}
     * USER   → rate_limit:{key}:user:{userId}
     * IP     → rate_limit:{key}:ip:{clientIp}
     */
    private String buildKey(RateLimit annotation) {
        String base = KEY_PREFIX + annotation.key();
        return switch (annotation.type()) {
            case USER -> {
                // 按用户 ID 隔离，未认证时以 "anonymous" 兜底
                String userId = (currentUserProvider != null)
                        ? currentUserProvider.getCurrentUserId() : null;
                yield base + ":user:" + (userId != null ? userId : "anonymous");
            }
            case IP -> base + ":ip:" + resolveClientIp();
            // GLOBAL：所有请求共享同一计数器
            default -> base;
        };
    }

    /**
     * 从当前 HTTP 请求上下文中解析客户端真实 IP。
     * 依次尝试反向代理透传的请求头，回退到 TCP 连接远端地址。
     */
    private String resolveClientIp() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // 非 Web 场景（单元测试、定时任务）无法解析 IP
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个即真实客户端
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 将标准 TimeUnit 转换为 Redisson 的 RateIntervalUnit。
     */
    private RateIntervalUnit toRedissonUnit(java.util.concurrent.TimeUnit unit) {
        return switch (unit) {
            case MILLISECONDS -> RateIntervalUnit.MILLISECONDS;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.DAYS;
            // MINUTES / SECONDS 均映射到秒（最常用场景），分钟场景转换为等效秒数由调用方控制
            default -> RateIntervalUnit.SECONDS;
        };
    }
}
