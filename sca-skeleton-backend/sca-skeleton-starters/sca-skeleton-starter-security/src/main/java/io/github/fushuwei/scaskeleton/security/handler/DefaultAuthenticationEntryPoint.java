package io.github.fushuwei.scaskeleton.security.handler;

import tools.jackson.databind.json.JsonMapper;
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
 * Spring Security 401 Unauthorized 身份验证入口类
 *
 * @author Fu Wei
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 使用 Jackson 序列化 JSON 响应体
     */
    private final JsonMapper jsonMapper;

    /**
     * 处理未认证异常
     *
     * @param request       当前 HTTP 请求
     * @param response      当前 HTTP 响应
     * @param authException Spring Security 抛出的认证异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // 记录日志
        log.warn("[401 Unauthorized] 未经授权的访问，详情：uri={} method={} message={}",
            request.getRequestURI(), request.getMethod(), authException.getMessage());

        // 构造统一错误响应体
        Result<Void> result = Result.fail(ResultCode.UNAUTHORIZED);

        // 设置响应为 JSON 格式，UTF-8 编码
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 将响应体序列化为 JSON 写入响应流
        response.getWriter().write(jsonMapper.writeValueAsString(result));
    }
}
