package io.github.fushuwei.scaskeleton.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * OIDC/JWT RSA 密钥配置（对应 {@code sca.auth.jwt.*}）。
 * <p>
 * 生产环境应通过配置中心注入 {@code private-key}/{@code public-key}/{@code key-id}；
 * 未配置时在本地开发环境内存生成临时密钥对。
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.auth.jwt")
public class AuthJwtProperties {

    /** JWK key id（kid），生产环境应保持稳定 */
    private String keyId;

    /** PKCS#8 PEM 或 Base64 私钥 */
    private String privateKey;

    /** X.509 PEM 或 Base64 公钥 */
    private String publicKey;

    /** 内存生成密钥时的 RSA 长度 */
    private int keySize = 2048;

    /**
     * 是否已配置外部持久化密钥。
     */
    public boolean isExternalKeyConfigured() {
        return StringUtils.hasText(privateKey) && StringUtils.hasText(publicKey);
    }
}
