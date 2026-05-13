package io.github.fushuwei.sca.starter.security.handler;

import tools.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.core.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Security 权限不足处理器。
 * <p>
 * 当已认证用户访问其无权限的资源时（HTTP 403），拦截 Spring Security 抛出的
 * {@link AccessDeniedException}，返回统一 JSON 格式的错误响应，
 * 而不是 Spring Security 默认的 403 HTML 页面。
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    // 使用 Jackson 序列化 JSON 响应体
    private final ObjectMapper objectMapper;

    /**
     * 处理权限不足异常，返回统一 JSON 格式的 403 错误响应。
     *
     * @param request               当前 HTTP 请求
     * @param response              当前 HTTP 响应
     * @param accessDeniedException Spring Security 抛出的权限异常
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        // 记录权限不足日志，方便排查权限配置问题
        log.warn("[Security] access denied. uri={} method={} message={}",
                request.getRequestURI(), request.getMethod(), accessDeniedException.getMessage());

        // 构造统一错误响应体，与 ApiResponse 格式对齐
        Map<String, Object> body = Map.of(
                "code", ErrorCode.FORBIDDEN.getCode(),
                "message", ErrorCode.FORBIDDEN.getMessage(),
                "data", null
        );

        // 设置响应为 JSON 格式，UTF-8 编码，HTTP 状态码 403
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 将响应体序列化为 JSON 写入响应流
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
