package io.github.fushuwei.scaskeleton.captcha.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 验证码响应结果类
 *
 * @author Fu Wei
 */
@Data
@AllArgsConstructor
public class CaptchaResult {

    /**
     * 验证码唯一标识
     */
    private String captchaKey;

    /**
     * 验证码图片的 Base64 编码字符串
     */
    private String imageBase64;
}
