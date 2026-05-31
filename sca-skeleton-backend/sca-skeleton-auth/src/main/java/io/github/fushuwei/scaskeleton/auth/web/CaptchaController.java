package io.github.fushuwei.scaskeleton.auth.web;

import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.captcha.model.CaptchaResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Map;

/**
 * 验证码控制器：提供图形验证码生成。
 * <p>
 * 支持两种响应模式：
 * <ul>
 *   <li>默认：直接输出 PNG 图片流（适合 {@code <img src="...">}）</li>
 *   <li>JSON：Accept 头包含 {@code application/json} 时返回 Base64（适合 AJAX）</li>
 * </ul>
 * <p>
 * 同时兼容网关 StripPrefix 后的路径 {@code /captcha/generate} 和直连路径 {@code /auth/captcha/generate}。
 *
 * @author Fu Wei
 */
@Controller
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    private final ObjectMapper objectMapper;

    /**
     * 生成图形验证码（接受 Accept 头自动切换响应格式）。
     */
    @GetMapping({"/captcha/generate", "/auth/captcha/generate"})
    public void generate(@RequestParam("key") String captchaKey,
                         HttpServletResponse response,
                         @RequestHeader(value = "Accept", defaultValue = "") String accept) throws IOException {
        // JSON 响应：返回 Base64 编码的验证码图片
        if (accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            CaptchaResult result = captchaService.generateBase64(captchaKey);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "captchaKey", result.getCaptchaKey(),
                    "imageBase64", result.getImageBase64()
            ));
            return;
        }
        // 默认响应：直接输出 PNG 图片流
        captchaService.generate(captchaKey, response);
    }
}
