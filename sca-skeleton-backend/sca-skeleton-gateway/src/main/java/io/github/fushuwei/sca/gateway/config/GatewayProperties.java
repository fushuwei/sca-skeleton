package io.github.fushuwei.sca.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关安全配置属性（绑定 application.yml sca.gateway.*）。
 *
 * @author Fu Wei
 */
@Configuration
@ConfigurationProperties(prefix = "sca.gateway")
public class GatewayProperties {

    /**
     * 白名单路径列表（Ant 风格），这些路径无需携带 JWT 即可访问。
     * 示例：/auth/oauth2/token, /auth/captcha/**, /actuator/health
     */
    private List<String> whiteList = new ArrayList<>();

    public List<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(List<String> whiteList) {
        this.whiteList = whiteList;
    }
}
