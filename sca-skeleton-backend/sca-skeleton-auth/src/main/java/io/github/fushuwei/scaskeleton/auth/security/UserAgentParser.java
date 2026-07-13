package io.github.fushuwei.scaskeleton.auth.security;

/**
 * 轻量级 User-Agent 解析器：从 UA 字符串提取设备类型、浏览器、操作系统。
 * <p>
 * 不依赖第三方库，覆盖常见浏览器与操作系统，无法识别时返回 "Unknown"。
 *
 * @author Fu Wei
 */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    /**
     * 解析 User-Agent 字符串。
     *
     * @param userAgent HTTP User-Agent 请求头
     * @return 包含 device、browser、os 的数组，顺序固定
     */
    public static String[] parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new String[]{"Unknown", "Unknown", "Unknown"};
        }
        // 统一小写后做关键字匹配
        String ua = userAgent.toLowerCase();
        return new String[]{resolveDevice(ua), resolveBrowser(ua), resolveOs(ua)};
    }

    /**
     * 识别设备类型：Mobile / Tablet / PC。
     */
    private static String resolveDevice(String ua) {
        // iPad 新版 UA 包含 "Mac OS" 但需优先判定为平板
        if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("playbook")
                || (ua.contains("android") && !ua.contains("mobile"))) {
            return "Tablet";
        }
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("android")
                || ua.contains("windows phone") || ua.contains("blackberry")) {
            return "Mobile";
        }
        return "PC";
    }

    /**
     * 识别浏览器：Edge 须在 Chrome 之前判定（新版 Edge UA 含 Chrome 关键字）。
     */
    private static String resolveBrowser(String ua) {
        if (ua.contains("edg")) {
            return "Edge";
        }
        if (ua.contains("opr") || ua.contains("opera")) {
            return "Opera";
        }
        if (ua.contains("chrome") && !ua.contains("chromium")) {
            return "Chrome";
        }
        if (ua.contains("chromium")) {
            return "Chromium";
        }
        if (ua.contains("firefox")) {
            return "Firefox";
        }
        if (ua.contains("safari")) {
            return "Safari";
        }
        return "Unknown";
    }

    /**
     * 识别操作系统。
     */
    private static String resolveOs(String ua) {
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "Unknown";
    }
}
