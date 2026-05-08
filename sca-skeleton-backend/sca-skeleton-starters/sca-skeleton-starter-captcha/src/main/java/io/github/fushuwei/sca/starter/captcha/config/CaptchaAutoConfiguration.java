package io.github.fushuwei.sca.starter.captcha.config;

import io.github.fushuwei.sca.starter.captcha.service.CaptchaService;
import io.github.fushuwei.sca.starter.captcha.service.DefaultCaptchaService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 验证码自动配置。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaAutoConfiguration {

    // 注册默认验证码服务实现。
    @Bean
    @ConditionalOnMissingBean(CaptchaService.class)
    public CaptchaService captchaService(CaptchaProperties captchaProperties) {
        // 创建基于配置的默认验证码服务实例。
        return new DefaultCaptchaService(captchaProperties);
    }
}
