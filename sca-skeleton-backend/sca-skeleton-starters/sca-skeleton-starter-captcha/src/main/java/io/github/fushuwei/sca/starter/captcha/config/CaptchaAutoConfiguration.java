package io.github.fushuwei.sca.starter.captcha.config;

import io.github.fushuwei.sca.starter.captcha.CaptchaService;
import io.github.fushuwei.sca.starter.captcha.impl.ImageCaptchaServiceImpl;
import io.github.fushuwei.sca.starter.captcha.properties.CaptchaProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Captcha Starter 自动配置入口。
 * <p>
 * 注册图形验证码服务 Bean，默认实现为 {@link ImageCaptchaServiceImpl}（Java AWT 渲染）。
 * 业务模块可通过注册自定义 {@link CaptchaService} Bean 替换默认实现。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaAutoConfiguration {

    /**
     * 注册图形验证码服务，依赖 CaptchaProperties 和 StringRedisTemplate。
     *
     * @param properties          验证码配置属性
     * @param stringRedisTemplate Spring Data Redis 字符串模板
     * @return CaptchaService 实例
     */
    @Bean
    @ConditionalOnMissingBean(CaptchaService.class)
    public CaptchaService captchaService(CaptchaProperties properties,
                                         StringRedisTemplate stringRedisTemplate) {
        return new ImageCaptchaServiceImpl(properties, stringRedisTemplate);
    }
}
