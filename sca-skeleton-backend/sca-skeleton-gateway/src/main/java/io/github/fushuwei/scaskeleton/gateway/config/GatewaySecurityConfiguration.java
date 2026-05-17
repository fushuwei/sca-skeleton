package io.github.fushuwei.scaskeleton.gateway.config;

import io.github.fushuwei.scaskeleton.gateway.filter.RequestHeaderGovernanceGlobalFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 网关安全配置类
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewaySecurityProperties.class)
@RequiredArgsConstructor
public class GatewaySecurityConfiguration {

    /**
     * 注册请求头治理过滤器：清理伪造内部头、注入 TraceId / 起始时间。
     *
     * @return 全局过滤器实例
     */
    @Bean
    @Order
    public RequestHeaderGovernanceGlobalFilter requestHeaderGovernanceGlobalFilter() {
        return new RequestHeaderGovernanceGlobalFilter();
    }
}
