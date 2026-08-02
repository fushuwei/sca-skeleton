package io.github.fushuwei.scaskeleton.datasource.engine.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 凭据加密配置属性。
 *
 * @author Fu Wei
 */
@Component
@ConfigurationProperties(prefix = "sca.datasource.crypto")
@Data
public class CredentialCipherProperties {

    /** 加密密钥（生产环境通过环境变量/Nacos 注入） */
    private String secretKey = "sca-skeleton-dev-secret-key-2026";

    /** 密钥版本（密文前缀，支持轮换） */
    private String cipherVersion = "v1";
}
