package io.github.fushuwei.sca.starter.captcha.service;

import io.github.fushuwei.sca.starter.captcha.config.CaptchaProperties;
import io.github.fushuwei.sca.starter.captcha.model.CaptchaChallenge;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 默认验证码服务实现，基于 Java2D 生成 PNG，无第三方图形库依赖。
 *
 * @author Fu Wei
 */
public class DefaultCaptchaService implements CaptchaService {

    // 验证码配置属性。
    private final CaptchaProperties captchaProperties;
    // 安全随机源。
    private final SecureRandom secureRandom = new SecureRandom();
    // 字符集，排除易混淆字符。
    private static final String ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";

    // 通过配置属性构造验证码服务。
    public DefaultCaptchaService(CaptchaProperties captchaProperties) {
        // 保存验证码配置用于生成策略。
        this.captchaProperties = captchaProperties;
    }

    // 生成验证码及其图片内容。
    @Override
    public CaptchaChallenge generateChallenge() {
        // 读取宽高与长度。
        int width = captchaProperties.getWidth();
        int height = captchaProperties.getHeight();
        int length = captchaProperties.getLength();
        // 拼接随机明文。
        StringBuilder plain = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            plain.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        // 比对统一小写。
        String code = plain.toString().toLowerCase();
        // 绘制到缓冲图像。
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            // 干扰线。
            for (int i = 0; i < 6; i++) {
                graphics.setColor(new Color(secureRandom.nextInt(200), secureRandom.nextInt(200), secureRandom.nextInt(200)));
                graphics.drawLine(
                        secureRandom.nextInt(width),
                        secureRandom.nextInt(height),
                        secureRandom.nextInt(width),
                        secureRandom.nextInt(height));
            }
            graphics.setFont(new Font("SansSerif", Font.BOLD, Math.max(height - 10, 18)));
            int step = Math.max((width - 20) / Math.max(length, 1), 12);
            for (int i = 0; i < length; i++) {
                graphics.setColor(new Color(secureRandom.nextInt(120), secureRandom.nextInt(120), secureRandom.nextInt(120)));
                graphics.drawString(String.valueOf(plain.charAt(i)), 10 + i * step, height - 8);
            }
        } finally {
            graphics.dispose();
        }
        // 输出 PNG Base64。
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return new CaptchaChallenge(code, "data:image/png;base64," + base64);
        } catch (Exception exception) {
            // 图形编码失败时抛出运行时异常，由上层统一处理。
            throw new IllegalStateException("captcha encode failed", exception);
        }
    }
}
