package io.github.fushuwei.sca.starter.captcha.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 验证码配置属性。
 *
 * @author Fu Wei
 */
@ConfigurationProperties(prefix = "security.captcha")
public class CaptchaProperties {

    // 验证码图片宽度。
    private int width = 130;
    // 验证码图片高度。
    private int height = 48;
    // 验证码字符数量。
    private int length = 4;

    // 获取验证码图片宽度。
    public int getWidth() {
        // 返回宽度配置。
        return width;
    }

    // 设置验证码图片宽度。
    public void setWidth(int width) {
        // 写入宽度配置。
        this.width = width;
    }

    // 获取验证码图片高度。
    public int getHeight() {
        // 返回高度配置。
        return height;
    }

    // 设置验证码图片高度。
    public void setHeight(int height) {
        // 写入高度配置。
        this.height = height;
    }

    // 获取验证码字符长度。
    public int getLength() {
        // 返回字符长度配置。
        return length;
    }

    // 设置验证码字符长度。
    public void setLength(int length) {
        // 写入字符长度配置。
        this.length = length;
    }
}
