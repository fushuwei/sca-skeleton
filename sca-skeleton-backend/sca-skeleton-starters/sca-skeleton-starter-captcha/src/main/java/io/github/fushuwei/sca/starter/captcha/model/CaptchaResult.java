package io.github.fushuwei.sca.starter.captcha.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 验证码生成结果（Base64 模式）。
 *
 * @author Fu Wei
 */
@Data
@AllArgsConstructor
public class CaptchaResult {

    /** 验证码唯一标识，前端校验时需回传此 key */
    private String captchaKey;

    /** 验证码图片的 Base64 编码字符串，格式：data:image/png;base64,{base64} */
    private String imageBase64;
}
