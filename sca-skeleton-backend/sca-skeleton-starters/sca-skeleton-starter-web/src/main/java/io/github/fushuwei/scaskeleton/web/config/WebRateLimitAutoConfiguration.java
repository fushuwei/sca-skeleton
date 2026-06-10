package io.github.fushuwei.scaskeleton.web.config;

import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.web.aspect.RateLimitAspect;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.jspecify.annotations.Nullable;

/**
 * Web 限流自动配置。
 * <p>
 * 在 Redisson 可用时注册 {@link RateLimitAspect}，支持 {@code @RateLimit} 注解驱动的分布式限流。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnBean(RedissonClient.class)
public class WebRateLimitAutoConfiguration {

    /**
     * 注册限流切面 Bean，依赖 RedissonClient 与可选的 CurrentUserProvider。
     *
     * @param redissonClient      Redisson 客户端
     * @param currentUserProvider 当前用户信息提供者（可选，由 Security Starter 提供）
     * @return RateLimitAspect 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(
            RedissonClient redissonClient,
            @Autowired(required = false) @Nullable CurrentUserProvider currentUserProvider) {
        // 组装限流切面：Redisson 提供分布式计数，CurrentUserProvider 可选用于按用户维度限流
        return new RateLimitAspect(redissonClient, currentUserProvider);
    }
}
