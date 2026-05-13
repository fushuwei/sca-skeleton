package io.github.fushuwei.scaskeleton.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * 本地内存缓存自动配置。
 * <p>
 * 基于 Caffeine 实现的高性能本地缓存，提供 TTL、最大容量等生产级配置，
 * 支持 {@code @Cacheable} 注解及 {@code CacheManager} 编程式存取。
 * <p>
 * 引入方式：确保 classpath 包含以下依赖（均为 optional，由消费方自行决定是否引入）：
 * <ul>
 *   <li>{@code spring-boot-starter-cache}</li>
 *   <li>{@code caffeine}</li>
 * </ul>
 *
 * @author Fu Wei
 */
@AutoConfiguration
@ConditionalOnClass(Caffeine.class)
public class LocalCacheConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LocalCacheConfiguration.class);

    /**
     * 生产级 Caffeine 缓存管理器。
     * <p>
     * 配置说明：
     * <ul>
     *   <li>{@code initialCapacity} —— 初始容量，避免频繁扩容</li>
     *   <li>{@code maximumSize} —— 最大条目数，防止内存溢出</li>
     *   <li>{@code expireAfterWrite} —— 写入后过期，保证数据最终一致性</li>
     *   <li>{@code recordStats} —— 开启统计，配合 Actuator {@code /actuator/caches} 监控</li>
     * </ul>
     *
     * @return CaffeineCacheManager 实例
     */
    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        log.info("Initializing CaffeineCacheManager with production configuration");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setCaffeine(Caffeine.newBuilder()
            // 初始容量：根据业务预估的缓存条目数设定，避免频繁 resize
            .initialCapacity(100)
            // 最大条目数：硬上限，防止缓存无限制增长导致 OOM
            .maximumSize(10_000)
            // 写入后 30 分钟过期：业务数据允许的最终一致时间窗口
            .expireAfterWrite(30, TimeUnit.MINUTES)
            // 写入后 10 分钟未被访问则过期：淘汰热点下降的数据
            .expireAfterAccess(10, TimeUnit.MINUTES)
            // 开启缓存命中率统计，便于生产监控
            .recordStats());

        // 允许缓存 null 值：避免缓存穿透时反复查询 DB
        cacheManager.setAllowNullValues(true);

        log.info("CaffeineCacheManager initialized successfully");
        return cacheManager;
    }
}
