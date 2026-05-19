package io.github.fushuwei.scaskeleton.captcha.impl;

import io.github.fushuwei.scaskeleton.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.captcha.model.CaptchaResult;
import io.github.fushuwei.scaskeleton.captcha.properties.CaptchaProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务实现。
 * <p>
 * 基于 Java AWT 生成随机干扰线和字符验证码图片，无需引入外部图形库。
 * 验证码文本以小写形式存入 Redis，校验时忽略大小写。
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class ImageCaptchaServiceImpl implements CaptchaService {

    // 验证码配置属性
    private final CaptchaProperties properties;

    // 用于存储验证码文本，使用 StringRedisTemplate 避免引入 JSON 序列化开销
    private final StringRedisTemplate stringRedisTemplate;

    private static final Random RANDOM = new Random();

    /**
     * 生成验证码并写入 HTTP 响应流（PNG 格式）。
     * 响应头设置为 image/png 并禁用缓存，确保每次请求都返回新的验证码图片。
     *
     * @param captchaKey 验证码唯一标识
     * @param response   HTTP 响应对象
     */
    @Override
    public void generate(String captchaKey, HttpServletResponse response) {
        // 生成随机验证码文本
        String code = generateCode();
        // 将验证码文本存入 Redis，统一转小写便于后续不区分大小写校验
        storeToRedis(captchaKey, code);

        // 设置响应头，防止浏览器缓存验证码图片
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setHeader("Pragma", "no-cache");

        // 渲染验证码图片并写入响应流
        try {
            BufferedImage image = renderImage(code);
            ImageIO.write(image, "PNG", response.getOutputStream());
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
        // 生成随机验证码文本
        String code = generateCode();
        // 存入 Redis
        storeToRedis(captchaKey, code);

        // 渲染图片并转换为 Base64 字符串
        try {
            BufferedImage image = renderImage(code);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            // 构造 data URI 格式的 Base64 字符串，前端可直接作为 img src 使用
            String base64 = "data:image/png;base64," +
                    Base64.getEncoder().encodeToString(baos.toByteArray());
            return new CaptchaResult(captchaKey, base64);
        } catch (IOException e) {
            log.error("[Captcha] failed to encode image to base64. key={}", captchaKey, e);
            throw new RuntimeException("验证码生成失败", e);
        }
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

        String redisKey = properties.getRedisKeyPrefix() + captchaKey;
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
     * 从字符池中随机生成指定长度的验证码文本。
     *
     * @return 验证码字符串
     */
    private String generateCode() {
        String charPool = properties.getCharPool();
        StringBuilder sb = new StringBuilder(properties.getCodeLength());
        for (int i = 0; i < properties.getCodeLength(); i++) {
            sb.append(charPool.charAt(RANDOM.nextInt(charPool.length())));
        }
        return sb.toString();
    }

    /**
     * 将验证码文本（小写）存入 Redis，并设置过期时间。
     *
     * @param captchaKey 验证码 Key
     * @param code       验证码明文
     */
    private void storeToRedis(String captchaKey, String code) {
        String redisKey = properties.getRedisKeyPrefix() + captchaKey;
        stringRedisTemplate.opsForValue().set(
                redisKey,
                // 统一转小写，使校验时不区分大小写
                code.toLowerCase(),
                properties.getExpireSeconds(),
                TimeUnit.SECONDS
        );
    }

    /**
     * 使用 Java AWT 渲染验证码图片，包含随机背景色、字符和干扰线。
     *
     * @param code 验证码文本
     * @return 渲染好的 BufferedImage
     */
    private BufferedImage renderImage(String code) {
        int width = properties.getWidth();
        int height = properties.getHeight();

        // 创建 RGB 图像画布
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 开启抗锯齿，提升文字清晰度
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制浅色随机背景
        g.setColor(new Color(240 + RANDOM.nextInt(15), 240 + RANDOM.nextInt(15), 240 + RANDOM.nextInt(15)));
        g.fillRect(0, 0, width, height);

        // 绘制 5 条随机颜色干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(randomLightColor());
            g.setStroke(new BasicStroke(1.0f));
            g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                    RANDOM.nextInt(width), RANDOM.nextInt(height));
        }

        // 绘制验证码字符，每个字符使用不同颜色和微小旋转
        Font font = new Font("Arial", Font.BOLD, height - 10);
        g.setFont(font);
        int charWidth = width / code.length();
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomDarkColor());
            // 每个字符随机轻微旋转（-20° ~ 20°），增加识别难度
            double angle = (RANDOM.nextDouble() - 0.5) * Math.PI / 4.5;
            g.rotate(angle, charWidth * i + charWidth / 2.0, height / 2.0);
            g.drawString(String.valueOf(code.charAt(i)),
                    charWidth * i + (charWidth - g.getFontMetrics().charWidth(code.charAt(i))) / 2,
                    height - 8);
            // 旋转复位，避免影响后续字符
            g.rotate(-angle, charWidth * i + charWidth / 2.0, height / 2.0);
        }

        g.dispose();
        return image;
    }

    /** 生成随机浅色（用于干扰线），RGB 各分量均 > 150 */
    private Color randomLightColor() {
        return new Color(150 + RANDOM.nextInt(100),
                150 + RANDOM.nextInt(100),
                150 + RANDOM.nextInt(100));
    }

    /** 生成随机深色（用于验证码字符），RGB 各分量均 < 120 */
    private Color randomDarkColor() {
        return new Color(RANDOM.nextInt(120),
                RANDOM.nextInt(120),
                RANDOM.nextInt(120));
    }
}
