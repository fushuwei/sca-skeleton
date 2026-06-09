package io.github.fushuwei.scaskeleton.captcha;

import io.github.fushuwei.scaskeleton.captcha.model.CaptchaResult;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 验证码服务接口
 *
 * @author Fu Wei
 */
public interface CaptchaService {

    /**
     * 生成验证码并将图片写入 HTTP 响应流，同时将验证码文本存入 Redis
     *
     * @param captchaKey 验证码唯一标识
     * @param response   HTTP 响应对象，用于输出图片流
     */
    void generate(String captchaKey, HttpServletResponse response);

    /**
     * 生成验证码并返回 Base64 编码结果（适合前后端分离场景，不直接写流）
     *
     * @param captchaKey 验证码唯一标识
     * @return 包含 Base64 图片数据和 key 的结果对象
     */
    CaptchaResult generateBase64(String captchaKey);

    /**
     * 校验验证码，无论校验成功还是失败，均会删除 Redis 中对应的验证码（一次性使用）
     *
     * @param captchaKey  验证码唯一标识
     * @param captchaCode 用户输入的验证码文本
     * @return true 表示验证通过，false 表示验证码错误或已过期
     */
    boolean verify(String captchaKey, String captchaCode);
}
