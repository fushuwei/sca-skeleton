package io.github.fushuwei.scaskeleton.redis.config;

import io.github.fushuwei.scaskeleton.core.user.CurrentUserProvider;
import io.github.fushuwei.scaskeleton.redis.aspect.RateLimitAspect;
import io.github.fushuwei.scaskeleton.redis.util.RedisUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;

/**
 * Redis Starter 自动配置入口。
 * <p>
 * 统一注册：RedisTemplate 序列化配置、RedisUtils 工具 Bean、RateLimitAspect 限流切面。
 * 依赖 RedissonClient（由 redisson-spring-boot-starter 自动装配）。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@Import(RedisTemplateConfig.class)
@ConditionalOnClass(RedissonClient.class)
public class RedisAutoConfiguration {

    /**
     * 注册 RedisUtils 工具 Bean，依赖上方配置的 RedisTemplate。
     *
     * @param redisTemplate 由 RedisTemplateConfig 提供的 RedisTemplate
     * @return RedisUtils 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RedisUtils redisUtils(RedisTemplate<String, Object> redisTemplate) {
        return new RedisUtils(redisTemplate);
    }

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
        return new RateLimitAspect(redissonClient, currentUserProvider);
    }
}
