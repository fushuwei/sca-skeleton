package io.github.fushuwei.sca.auth.service;

import io.github.fushuwei.sca.auth.config.ScaAuthProperties;
import io.github.fushuwei.sca.starter.core.exception.BusinessException;
import io.github.fushuwei.sca.starter.core.exception.ErrorCode;
import io.github.fushuwei.sca.starter.core.id.UuidUtils;
import io.github.fushuwei.sca.starter.captcha.model.CaptchaChallenge;
import io.github.fushuwei.sca.starter.captcha.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 验证码票据签发与校验，答案仅存 Redis。
 *
 * @author Fu Wei
 */
@Service
@RequiredArgsConstructor
public class CaptchaTicketService {

    // Redis 字符串模板。
    private final StringRedisTemplate stringRedisTemplate;
    // 图形验证码生成。
    private final CaptchaService captchaService;
    // TTL 配置。
    private final ScaAuthProperties authProperties;
    // Redis 键前缀。
    private static final String KEY_PREFIX = "auth:captcha:";

    // 生成验证码票据并缓存答案。
    public CaptchaTicket issue() {
        // 生成随机票据 ID。
        String ticketId = UuidUtils.nextSimpleStr();
        // 调用图形引擎生成挑战。
        CaptchaChallenge challenge = captchaService.generateChallenge();
        // 写入 Redis，统一小写比对。
        stringRedisTemplate.opsForValue()
                .set(KEY_PREFIX + ticketId, challenge.code(), Duration.ofSeconds(authProperties.getCaptchaTtlSeconds()));
        // 仅向客户端暴露图片，不泄露答案。
        return new CaptchaTicket(ticketId, challenge.imageBase64());
    }

    // 消费验证码票据，失败抛出业务异常。
    public void validateAndConsume(String ticketId, String userInput) {
        // 读取缓存答案。
        String answer = stringRedisTemplate.opsForValue().get(KEY_PREFIX + ticketId);
        // 不存在视为过期或伪造。
        if (answer == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "captcha expired");
        }
        // 一次性删除，防止重放。
        stringRedisTemplate.delete(KEY_PREFIX + ticketId);
        // 用户输入为空直接失败。
        if (userInput == null || userInput.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "captcha required");
        }
        // 忽略大小写比对。
        if (!answer.equalsIgnoreCase(userInput.trim())) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "captcha mismatch");
        }
    }

    /**
     * 对外返回的验证码载荷。
     *
     * @param ticketId 票据 ID
     * @param image    前端可直接展示的 data URL
     */
    public record CaptchaTicket(String ticketId, String image) {
    }
}
