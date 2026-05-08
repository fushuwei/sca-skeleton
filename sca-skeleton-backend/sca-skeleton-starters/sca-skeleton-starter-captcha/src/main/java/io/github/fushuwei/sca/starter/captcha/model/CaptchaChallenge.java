package io.github.fushuwei.sca.starter.captcha.model;

/**
 * 验证码挑战对象。
 *
 * @author Fu Wei
 */
public record CaptchaChallenge(String code, String imageBase64) {
}
