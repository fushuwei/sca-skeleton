package io.github.fushuwei.sca.gateway.config;

import io.github.fushuwei.sca.gateway.filter.RequestHeaderGovernanceGlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关配置类
 *
 * @author Fu Wei
 */
@Configuration
public class GatewayConfig {

    /**
     * 网关全局请求头治理过滤器
     */
    @Bean
    public RequestHeaderGovernanceGlobalFilter requestHeaderGovernanceGlobalFilter() {
        return new RequestHeaderGovernanceGlobalFilter();
    }
}
