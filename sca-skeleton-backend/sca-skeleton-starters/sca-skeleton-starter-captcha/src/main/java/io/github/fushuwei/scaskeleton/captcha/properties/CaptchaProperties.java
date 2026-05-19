package io.github.fushuwei.scaskeleton.captcha.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 验证码配置属性。
 * <p>
 * 通过 {@code captcha.*} 前缀在 {@code application.yml} 中配置，例如：
 * <pre>
 * captcha:
 *   width: 120
 *   height: 40
 *   code-length: 4
 *   expire-seconds: 300
 * </pre>
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    /** 验证码图片宽度（像素），默认 120 */
    private int width = 120;

    /** 验证码图片高度（像素），默认 40 */
    private int height = 40;

    /** 验证码字符数量，默认 4 位 */
    private int codeLength = 4;

    /** 验证码 Redis 过期时间（秒），默认 5 分钟 */
    private long expireSeconds = 300;

    /** 验证码字符来源池，默认去掉容易混淆的字符（0/O、1/l/I） */
    private String charPool = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz";

    /** Redis Key 前缀 */
    private String redisKeyPrefix = "captcha:";
}
