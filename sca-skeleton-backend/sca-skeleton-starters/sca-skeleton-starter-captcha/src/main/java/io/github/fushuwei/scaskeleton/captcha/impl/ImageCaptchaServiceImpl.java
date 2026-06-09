package io.github.fushuwei.scaskeleton.captcha.impl;

import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.github.fushuwei.scaskeleton.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.captcha.model.CaptchaResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 图片验证码服务实现类
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class ImageCaptchaServiceImpl implements CaptchaService {

    /**
     * 验证码图片宽度（像素）
     */
    private final int captchaWidth;

    /**
     * 验证码图片高度（像素）
     */
    private final int captchaHeight;

    /**
     * 验证码字符数量
     */
    private final int codeLength;

    /**
     * 验证码 Redis 过期时间（秒）
     */
    private final long expireSeconds;

    /**
     * Redis Key 前缀
     */
    private final String redisKeyPrefix;

    /**
     * Redis 字符串操作模板（Key/Value 均为 String 类型）
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成验证码并写入 HTTP 响应流（PNG 格式）
     *
     * @param captchaKey 验证码唯一标识
     * @param response   HTTP 响应对象
     */
    @Override
    public void generate(String captchaKey, HttpServletResponse response) {
        SpecCaptcha captcha = createCaptcha();

        // 将验证码文本存入 Redis
        storeToRedis(captchaKey, captcha.text());

        // 设置响应头，防止浏览器缓存验证码图片
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setHeader("Pragma", "no-cache");

        // 渲染验证码图片并写入响应流
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            log.error("[验证码] 无法将验证码图片写入响应流. captchaKey={}", captchaKey, e);
        }
    }

    /**
     * 生成验证码并返回 Base64 编码结果
     *
     * @param captchaKey 验证码唯一标识
     * @return 包含 key 和 Base64 图片数据的结果对象
     */
    @Override
    public CaptchaResult generateBase64(String captchaKey) {
        SpecCaptcha captcha = createCaptcha();

        // 将验证码文本存入 Redis
        storeToRedis(captchaKey, captcha.text());

        // 返回 Base64 编码的图片数据
        return new CaptchaResult(captchaKey, captcha.toBase64());
    }

    /**
     * 校验验证码，无论校验成功还是失败，均会删除 Redis 中对应的验证码（一次性使用）
     *
     * @param captchaKey  验证码唯一标识
     * @param captchaCode 用户输入的验证码文本
     * @return true 表示验证通过，false 表示验证码错误或已过期
     */
    @Override
    public boolean verify(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }

        // 从 Redis 中获取存储的验证码
        String redisKey = redisKeyPrefix + ":image:" + captchaKey;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        // 删除 Redis Key，确保验证码一次性使用
        stringRedisTemplate.delete(redisKey);

        // 验证码不存在或已过期
        if (storedCode == null) {
            return false;
        }

        return storedCode.equalsIgnoreCase(captchaCode.trim());
    }

    /**
     * 创建 SpecCaptcha 实例
     *
     * @return 配置好的 SpecCaptcha 实例
     */
    private SpecCaptcha createCaptcha() {
        SpecCaptcha captcha = new SpecCaptcha(captchaWidth, captchaHeight, codeLength);
        captcha.setCharType(Captcha.TYPE_DEFAULT);
        return captcha;
    }

    /**
     * 将验证码文本（小写）存入 Redis，并设置过期时间
     *
     * @param captchaKey 验证码唯一标识
     * @param code       验证码明文（统一转小写，校验时不区分大小写）
     */
    private void storeToRedis(String captchaKey, String code) {
        String redisKey = redisKeyPrefix + ":image:" + captchaKey;
        stringRedisTemplate.opsForValue().set(
            redisKey,
            code.toLowerCase(),
            expireSeconds,
            TimeUnit.SECONDS
        );
    }
}
