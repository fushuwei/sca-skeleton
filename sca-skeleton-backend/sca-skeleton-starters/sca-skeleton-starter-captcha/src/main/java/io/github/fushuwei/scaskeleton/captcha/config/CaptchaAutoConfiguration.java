package io.github.fushuwei.scaskeleton.captcha.config;

import io.github.fushuwei.scaskeleton.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.captcha.impl.ImageCaptchaServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 验证码配置类
 *
 * @author Fu Wei
 */
@AutoConfiguration
@EnableConfigurationProperties(CaptchaProperties.class)
public class CaptchaAutoConfiguration {

    /**
     * 注册图形验证码服务
     *
     * @param properties          验证码配置属性
     * @param stringRedisTemplate Spring Data Redis 字符串模板
     * @return CaptchaService 实例
     */
    @Bean
    @ConditionalOnMissingBean(CaptchaService.class)
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
