package io.github.fushuwei.sca.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * 授权服务器签名密钥，开发环境进程内生成 RSA，生产应改为 KMS 或配置注入。
 *
 * @author Fu Wei
 */
@Configuration
public class RsaJwtKeyConfiguration {

    // 提供 RSA JWK，供 JWT 签发与 JWK Set 端点使用。
    @Bean
    public RSAKey authorizationServerRsaKey() throws Exception {
        // 使用 2048 位 RSA，满足通用安全基线。
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        // 初始化密钥长度。
        generator.initialize(2048);
        // 生成密钥对。
        KeyPair keyPair = generator.generateKeyPair();
        // 组装为 Nimbus RSAKey，包含 keyID 便于轮换。
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    // JWK 集合来源，供授权服务器与本地解码器使用。
    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        // 单密钥 JWKSet，后续可扩展为多密钥。
        JWKSet jwkSet = new JWKSet(rsaKey);
        // 不可变 JWK 源，避免运行期被篡改。
        return new ImmutableJWKSet<>(jwkSet);
    }

    // JWT 编码器，用于自定义登录接口签发访问令牌。
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        // Nimbus 实现与 Spring Security OAuth2 默认栈一致。
        return new NimbusJwtEncoder(jwkSource);
    }

}
