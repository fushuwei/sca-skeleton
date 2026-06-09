package io.github.fushuwei.scaskeleton.captcha.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 验证码配置属性
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "captcha")
public class CaptchaProperties {

    /**
     * 验证码图片宽度（像素），默认 120
     */
    private int width = 120;

    /**
     * 验证码图片高度（像素），默认 44
     */
    private int height = 44;

    /**
     * 验证码字符数量，默认 4 位
     */
    private int codeLength = 4;

    /**
     * 验证码 Redis 过期时间（秒），默认 2 分钟
     */
    private long expireSeconds = 120;

    /**
     * Redis Key 前缀
     */
    private String redisKeyPrefix = "captcha:";
}
