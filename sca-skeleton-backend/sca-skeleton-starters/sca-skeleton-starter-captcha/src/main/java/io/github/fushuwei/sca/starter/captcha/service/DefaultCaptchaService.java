package io.github.fushuwei.sca.starter.captcha.service;

import com.wf.captcha.SpecCaptcha;
import io.github.fushuwei.sca.starter.captcha.config.CaptchaProperties;
import io.github.fushuwei.sca.starter.captcha.model.CaptchaChallenge;
import org.springframework.util.Base64Utils;

import java.nio.charset.StandardCharsets;

/**
 * 默认验证码服务实现。
 *
 * @author Fu Wei
 */
public class DefaultCaptchaService implements CaptchaService {

    // 验证码配置属性。
    private final CaptchaProperties captchaProperties;

    // 通过配置属性构造验证码服务。
    public DefaultCaptchaService(CaptchaProperties captchaProperties) {
        // 保存验证码配置用于生成策略。
        this.captchaProperties = captchaProperties;
    }

    // 生成验证码及其图片内容。
    @Override
    public CaptchaChallenge generateChallenge() {
        // 基于配置创建图形验证码对象。
        SpecCaptcha captcha = new SpecCaptcha(captchaProperties.getWidth(), captchaProperties.getHeight(), captchaProperties.getLength());
        // 使用默认样式输出验证码文本。
        captcha.setCharType(SpecCaptcha.TYPE_DEFAULT);
        // 获取验证码文本并统一转小写用于比对。
        String code = captcha.text().toLowerCase();
        // 组装 data url 前缀。
        String dataPrefix = "data:image/png;base64,";
        // 对验证码图片内容执行 Base64 编码。
        String imageBase64 = Base64Utils.encodeToString(captcha.toBase64().getBytes(StandardCharsets.UTF_8));
        // 返回验证码文本和图片内容。
        return new CaptchaChallenge(code, dataPrefix + imageBase64);
    }
}
