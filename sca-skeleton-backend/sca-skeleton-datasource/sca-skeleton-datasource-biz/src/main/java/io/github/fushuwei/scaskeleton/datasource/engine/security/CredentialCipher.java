package io.github.fushuwei.scaskeleton.datasource.engine.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 凭据加密器（AES-GCM + 密钥版本前缀）。
 * <p>
 * 密文格式：{@code cipher_version:base64(nonce + ciphertext + tag)}
 * <p>
 * 安全约束（对应设计方案 v1.3 §6 凭据加密）：
 * - 每条密文独立随机 12 字节 nonce；
 * - 密钥版本前缀支持轮换（新版本密钥可解密旧密文，或重加密任务）；
 * - 错误信息、日志、连接串回显禁止包含密码或密文。
 *
 * @author Fu Wei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CredentialCipher {

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final CredentialCipherProperties properties;

    /**
     * 加密明文密码。
     *
     * @param plaintext 明文密码
     * @return 密文（cipher_version:base64(nonce+ciphertext+tag)）
     */
    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            new SecureRandom().nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(ciphertext, 0, combined, nonce.length, ciphertext.length);

            return properties.getCipherVersion() + ":" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("凭据加密失败", e);
        }
    }

    /**
     * 解密密文。
     *
     * @param ciphertext 密文（cipher_version:base64(nonce+ciphertext+tag)）
     * @return 明文密码
     */
    public String decrypt(String ciphertext) {
        try {
            int colonIndex = ciphertext.indexOf(':');
            if (colonIndex < 0) {
                throw new IllegalArgumentException("密文格式错误：缺少版本前缀");
            }
            // 密钥版本 = ciphertext.substring(0, colonIndex)，当前只支持一个版本
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(colonIndex + 1));
            byte[] nonce = Arrays.copyOf(combined, NONCE_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, NONCE_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("凭据解密失败", e);
        }
    }

    /**
     * 获取当前密钥版本（用于入库时写入 cipher_version 字段）。
     *
     * @return 密钥版本字符串
     */
    public String currentCipherVersion() {
        return properties.getCipherVersion();
    }

    /**
     * 从配置的 secretKey 派生 AES-256 密钥（SHA-256）。
     */
    private SecretKey deriveKey() throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256")
            .digest(properties.getSecretKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hash, "AES");
    }
}
