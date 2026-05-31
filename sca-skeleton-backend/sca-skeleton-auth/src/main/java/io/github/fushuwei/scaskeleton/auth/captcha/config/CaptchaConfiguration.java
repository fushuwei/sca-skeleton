package io.github.fushuwei.scaskeleton.auth.captcha.config;

import io.github.fushuwei.scaskeleton.auth.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.auth.captcha.impl.ImageCaptchaServiceImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 验证码配置类。
 * <p>
 * 注册 {@link CaptchaService} Bean，使用 pig4cloud captcha-core（SpecCaptcha）
 * 作为底层渲染引擎，替代原有 {@code sca-skeleton-starter-captcha} 中的 AWT 实现。
 * 配置项通过 {@code auth.captcha.*} 前缀进行管理。
 *
 * @author Fu Wei
 */
@Configuration(proxyBeanMethods = false)
public class CaptchaConfiguration {

    /**
     * 验证码配置属性 Bean。
     */
    @Bean
    @ConfigurationProperties(prefix = "sca.captcha")
    public CaptchaProperties captchaProperties() {
        return new CaptchaProperties();
    }

    /**
     * 注册图形验证码服务。
     *
     * @param properties          验证码配置属性
     * @param stringRedisTemplate Spring Data Redis 字符串模板
     * @return CaptchaService 实例
     */
    @Bean
    public CaptchaService captchaService(CaptchaProperties properties,
                                         StringRedisTemplate stringRedisTemplate) {
        return new ImageCaptchaServiceImpl(
                properties.getWidth(),
                properties.getHeight(),
                properties.getCodeLength(),
                properties.getExpireSeconds(),
                properties.getRedisKeyPrefix(),
                stringRedisTemplate
        );
    }
}
