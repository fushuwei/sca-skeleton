package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.captcha.CaptchaService;
import io.github.fushuwei.scaskeleton.captcha.model.CaptchaResult;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;
import java.util.Map;

/**
 * 验证码控制器：提供图形验证码生成
 * <p>
 * 支持两种响应模式：
 * <ul>
 *   <li>默认：直接输出 PNG 图片流（适合 {@code <img src="...">}）</li>
 *   <li>JSON：Accept 头包含 {@code application/json} 时返回 Base64（适合 AJAX）</li>
 * </ul>
 * <p>
 * 前端统一通过网关访问：{@code /api/auth/captcha/generate} → 网关 StripPrefix=1 → {@code /captcha/generate}
 *
 * @author Fu Wei
 */
@Controller
@RequiredArgsConstructor
public class CaptchaController {

    /**
     * PNG 模式下返回 captchaKey 的响应头名称
     */
    private static final String HEADER_CAPTCHA_KEY = "X-Captcha-Key";

    private final CaptchaService captchaService;

    private final JsonMapper jsonMapper;

    /**
     * 生成图形验证码（接受 Accept 头自动切换响应格式）
     * <p>
     * captchaKey 在服务端生成，确保客户端无法预测或控制验证码标识
     */
    @GetMapping("/captcha/generate")
    public void generate(HttpServletResponse response,
                         @RequestHeader(value = "Accept", defaultValue = "") String accept) throws IOException {
        // 服务端生成 captchaKey
        String captchaKey = UuidUtils.v4SimpleStr();

        // JSON 响应：返回 Base64 编码的验证码图片
        if (accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            CaptchaResult result = captchaService.generateBase64(captchaKey);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            jsonMapper.writeValue(response.getWriter(), Map.of(
                "captchaKey", result.getCaptchaKey(),
                "imageBase64", result.getImageBase64()
            ));
            return;
        }

        // 默认响应：直接输出 PNG 图片流
        // 通过响应头返回 captchaKey，供 AJAX 调用方读取（<img src> 标签无法读取响应头，应改用 JSON 模式）
        response.setHeader(HEADER_CAPTCHA_KEY, captchaKey);
        captchaService.generate(captchaKey, response);
    }
}
