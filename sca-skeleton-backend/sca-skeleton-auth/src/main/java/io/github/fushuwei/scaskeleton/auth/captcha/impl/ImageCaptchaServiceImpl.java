package io.github.fushuwei.scaskeleton.auth.captcha.impl;

import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.github.fushuwei.scaskeleton.auth.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.auth.captcha.model.CaptchaResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现类
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class ImageCaptchaServiceImpl implements CaptchaService {

    /** 验证码图片宽度（像素） */
    private final int captchaWidth;

    /** 验证码图片高度（像素） */
    private final int captchaHeight;

    /** 验证码字符数量 */
    private final int codeLength;

    /** 验证码 Redis 过期时间（秒） */
    private final long expireSeconds;

    /** Redis Key 前缀 */
    private final String redisKeyPrefix;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成验证码并写入 HTTP 响应流（PNG 格式）。
     * <p>
     * 响应头设置为 image/png 并禁用缓存，确保每次请求都返回新的验证码图片。
     *
     * @param captchaKey 验证码唯一标识
     * @param response   HTTP 响应对象
     */
    @Override
    public void generate(String captchaKey, HttpServletResponse response) {
        SpecCaptcha captcha = createCaptcha();
        // 将验证码文本存入 Redis，统一转小写便于后续不区分大小写校验
        storeToRedis(captchaKey, captcha.text());

        // 设置响应头，防止浏览器缓存验证码图片
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setHeader("Pragma", "no-cache");

        // 渲染验证码图片并写入响应流
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            log.error("[Captcha] failed to write image to response. key={}", captchaKey, e);
        }
    }

    /**
     * 生成验证码并返回 Base64 编码结果，适合前后端分离的 JSON 接口场景。
     *
     * @param captchaKey 验证码唯一标识
     * @return 包含 key 和 Base64 图片数据的结果对象
     */
    @Override
    public CaptchaResult generateBase64(String captchaKey) {
        SpecCaptcha captcha = createCaptcha();
        // 存入 Redis
        storeToRedis(captchaKey, captcha.text());
        // 获取 Base64 编码的图片数据
        return new CaptchaResult(captchaKey, captcha.toBase64());
    }

    /**
     * 校验验证码，校验后无论成功或失败均删除 Redis 中的记录（一次性使用）。
     *
     * @param captchaKey  验证码唯一标识
     * @param captchaCode 用户输入的验证码文本
     * @return true 表示验证通过
     */
    @Override
    public boolean verify(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }

        String redisKey = redisKeyPrefix + captchaKey;
        // 从 Redis 中获取存储的验证码（已转小写）
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        // 无论校验结果如何，均删除 Redis Key，确保验证码一次性使用
        stringRedisTemplate.delete(redisKey);

        if (storedCode == null) {
            // 验证码不存在或已过期
            return false;
        }

        // 忽略大小写进行比较
        return storedCode.equalsIgnoreCase(captchaCode.trim());
    }

    /**
     * 将验证码文本（小写）存入 Redis，并设置过期时间。
     *
     * @param captchaKey 验证码 Key
     * @param code       验证码明文
     */
    private void storeToRedis(String captchaKey, String code) {
        String redisKey = redisKeyPrefix + captchaKey;
        stringRedisTemplate.opsForValue().set(
                redisKey,
                // 统一转小写，使校验时不区分大小写
                code.toLowerCase(),
                expireSeconds,
                TimeUnit.SECONDS
        );
    }

    /**
     * 创建 kaptcha 风格的 SpecCaptcha 实例。
     * <p>
     * 使用 {@link Captcha#TYPE_ONLY_CHAR} 纯字母模式，与 kaptcha 风格一致。
     *
     * @return 配置好的 SpecCaptcha 实例
     */
    private SpecCaptcha createCaptcha() {
        SpecCaptcha captcha = new SpecCaptcha(captchaWidth, captchaHeight, codeLength);
        captcha.setCharType(Captcha.TYPE_ONLY_CHAR);
        return captcha;
    }
}
