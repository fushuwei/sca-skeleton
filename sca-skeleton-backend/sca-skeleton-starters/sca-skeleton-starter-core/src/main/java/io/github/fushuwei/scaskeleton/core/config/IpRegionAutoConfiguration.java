package io.github.fushuwei.scaskeleton.core.config;

import io.github.fushuwei.scaskeleton.core.support.Ip2RegionResolver;
import io.github.fushuwei.scaskeleton.core.support.IpRegionResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IP 地理位置解析自动配置类
 *
 * @author Fu Wei
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(IpRegionProperties.class)
public class IpRegionAutoConfiguration {

    /**
     * IP 地理位置解析器
     */
    @Bean
    @ConditionalOnMissingBean(IpRegionResolver.class)
    public IpRegionResolver ipRegionResolver(IpRegionProperties properties) {
        String dbPath = properties.getDbPath();
        if (!StringUtils.hasText(dbPath)) {
            log.warn("[IP解析] 未配置 ip2region 数据库路径，location 字段不会被解析");
            return ip -> null;
        }
        Path path = Path.of(dbPath);
        if (!Files.exists(path)) {
            log.warn("[IP解析] ip2region 数据库文件不存在: {}，location 字段不会被解析", dbPath);
            return ip -> null;
        }
        try {
            log.info("[IP解析] 加载 ip2region 数据库: {}", dbPath);
            return Ip2RegionResolver.create(dbPath);
        } catch (Exception e) {
            log.warn("[IP解析] ip2region 数据库加载失败，location 字段不会被解析", e);
            return ip -> null;
        }
    }
}
