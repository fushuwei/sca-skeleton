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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 403 Forbidden 处理器
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * 使用 Jackson 序列化 JSON 响应体
     */
    private final ObjectMapper objectMapper;

    /**
     * 处理拒绝访问异常
     *
     * @param request               当前 HTTP 请求
     * @param response              当前 HTTP 响应
     * @param accessDeniedException Spring Security 抛出的权限异常
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        // 记录日志
        log.warn("[403 Forbidden] 权限不足，拒绝访问，详情：uri={} method={} message={}",
            request.getRequestURI(), request.getMethod(), accessDeniedException.getMessage());

        // 构造统一错误响应体
        Result<Void> result = Result.fail(ResultCode.FORBIDDEN);

        // 设置响应为 JSON 格式，UTF-8 编码
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 将响应体序列化为 JSON 写入响应流
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
