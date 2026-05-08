package io.github.fushuwei.sca.starter.captcha.service;

import io.github.fushuwei.sca.starter.captcha.model.CaptchaChallenge;

/**
 * 验证码服务接口。
 *
 * @author Fu Wei
 */
public interface CaptchaService {

    // 生成验证码挑战内容。
    CaptchaChallenge generateChallenge();
}
