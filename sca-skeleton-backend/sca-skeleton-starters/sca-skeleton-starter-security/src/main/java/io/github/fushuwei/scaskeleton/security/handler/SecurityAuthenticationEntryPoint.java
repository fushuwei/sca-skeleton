package io.github.fushuwei.scaskeleton.security.handler;

import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.result.Result;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 未认证请求入口处理器。
 * <p>
 * 当未认证用户（无 Token 或 Token 无效）访问受保护资源时（HTTP 401），
 * 拦截 Spring Security 抛出的 {@link AuthenticationException}，
 * 返回统一 JSON 格式的 401 错误响应，而不是默认的重定向或 HTML 页面。
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // 使用 Jackson 序列化 JSON 响应体
    private final ObjectMapper objectMapper;

    /**
     * 处理未认证异常，返回统一 JSON 格式的 401 错误响应。
     *
     * @param request       当前 HTTP 请求
     * @param response      当前 HTTP 响应
     * @param authException Spring Security 抛出的认证异常
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // 记录未认证请求日志，Token 缺失或过期时有助于排查
        log.warn("[Security] unauthorized access. uri={} method={} message={}",
                request.getRequestURI(), request.getMethod(), authException.getMessage());

        // 构造统一错误响应体，与 Result 格式对齐
        Result<Void> body = Result.fail(ResultCode.UNAUTHORIZED);

        // 设置响应为 JSON 格式，UTF-8 编码，HTTP 状态码 401
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 将响应体序列化为 JSON 写入响应流
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
