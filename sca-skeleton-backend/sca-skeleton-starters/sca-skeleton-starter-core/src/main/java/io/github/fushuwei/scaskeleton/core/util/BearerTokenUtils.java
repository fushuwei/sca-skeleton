package io.github.fushuwei.scaskeleton.core.util;

import io.github.fushuwei.scaskeleton.core.constant.GlobalConstants;
import org.springframework.util.StringUtils;

/**
 * Bearer Token 解析工具类
 *
 * @author Fu Wei
 */
public final class BearerTokenUtils {

    private static final String BEARER_PREFIX = GlobalConstants.BEARER_PREFIX;

    private BearerTokenUtils() {
    }

    /**
     * 从 Authorization 请求头值中提取 Bearer Token
     *
     * @param authorizationHeader 请求头 Authorization 的完整值
     * @return Token 值；格式不符或为空时返回 null
     */
    public static String extractBearerToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }

        // 长度预判，避免 substring 越界
        if (authorizationHeader.length() < BEARER_PREFIX.length()) {
            return null;
        }

        // 忽略大小写匹配 "Bearer "
        if (authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            return StringUtils.hasText(token) ? token : null;
        }

        return null;
    }
}
