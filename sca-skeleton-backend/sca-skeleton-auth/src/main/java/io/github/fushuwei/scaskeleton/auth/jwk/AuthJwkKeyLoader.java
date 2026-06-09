package io.github.fushuwei.scaskeleton.auth.jwk;

import io.github.fushuwei.scaskeleton.auth.config.properties.AuthJwtProperties;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥加载器：优先使用外部化配置，否则在本地开发环境内存生成。
 *
 * @author Fu Wei
 */
@Component
@RequiredArgsConstructor
public class AuthJwkKeyLoader {

    /** JWT/JWK 外部化密钥配置 */
    private final AuthJwtProperties authJwtProperties;

    /**
     * 加载 RSA 密钥对。
     */
    public KeyPair loadKeyPair() {
        // 生产/测试：从配置中心注入 PEM 或 Base64 密钥
        if (authJwtProperties.isExternalKeyConfigured()) {
            return loadExternalKeyPair();
        }
        // 本地开发：内存生成临时密钥
        return generateInMemoryKeyPair();
    }

    /**
     * 解析 kid：外部配置优先，否则按项目 UUID 策略生成。
     */
    public String resolveKeyId() {
        if (StringUtils.hasText(authJwtProperties.getKeyId())) {
            return authJwtProperties.getKeyId();
        }
        return UuidUtils.nextSimpleStr();
    }

    /**
     * 从配置解析外部 RSA 密钥对。
     */
    private KeyPair loadExternalKeyPair() {
        try {
            RSAPrivateKey privateKey = parsePrivateKey(authJwtProperties.getPrivateKey());
            RSAPublicKey publicKey = parsePublicKey(authJwtProperties.getPublicKey());
            return new KeyPair(publicKey, privateKey);
        } catch (Exception ex) {
            throw new IllegalStateException("外部 RSA 密钥加载失败", ex);
        }
    }

    /**
     * 本地开发内存生成 RSA 密钥对。
     */
    private KeyPair generateInMemoryKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(authJwtProperties.getKeySize());
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("RSA 密钥对生成失败", ex);
        }
    }

    /**
     * 解析 PKCS#8 私钥（PEM 或纯 Base64）。
     */
    private RSAPrivateKey parsePrivateKey(String rawKey) throws Exception {
        byte[] decoded = decodePem(rawKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    /**
     * 解析 X.509 公钥（PEM 或纯 Base64）。
     */
    private RSAPublicKey parsePublicKey(String rawKey) throws Exception {
        byte[] decoded = decodePem(rawKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    /**
     * 去除 PEM 头尾并 Base64 解码。
     */
    private byte[] decodePem(String rawKey) {
        String normalized = rawKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
    }
}
