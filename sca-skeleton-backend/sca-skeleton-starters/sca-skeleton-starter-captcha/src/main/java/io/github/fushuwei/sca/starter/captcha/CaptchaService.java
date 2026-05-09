package io.github.fushuwei.sca.starter.captcha;

import io.github.fushuwei.sca.starter.captcha.model.CaptchaResult;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 验证码服务接口。
 * <p>
 * 定义验证码生成与校验的统一契约，默认实现为基于 Java AWT 的图形验证码。
 * 业务模块可通过自定义 Bean 替换为短信验证码、邮件验证码等实现。
 *
 * @author Fu Wei
 */
public interface CaptchaService {

    /**
     * 生成验证码并将图片写入 HTTP 响应流，同时将验证码文本存入 Redis。
     *
     * @param captchaKey 验证码唯一标识，建议使用 UUID，与校验时的 key 对应
     * @param response   HTTP 响应对象，用于输出图片流
     */
    void generate(String captchaKey, HttpServletResponse response);

    /**
     * 生成验证码并返回 Base64 编码结果（适合前后端分离场景，不直接写流）。
     *
     * @param captchaKey 验证码唯一标识
     * @return 包含 Base64 图片数据和 key 的结果对象
     */
    CaptchaResult generateBase64(String captchaKey);

    /**
     * 校验验证码。校验成功或失败后，均删除 Redis 中对应的验证码（一次性使用）。
     *
     * @param captchaKey  验证码唯一标识
     * @param captchaCode 用户输入的验证码文本
     * @return true 表示验证通过，false 表示验证码错误或已过期
     */
    boolean verify(String captchaKey, String captchaCode);
}
