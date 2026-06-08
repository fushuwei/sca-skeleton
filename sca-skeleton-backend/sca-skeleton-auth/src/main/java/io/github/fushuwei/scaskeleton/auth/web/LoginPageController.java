package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.AuthLoginProperties;
import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Auth 服务托管的登录页控制器（Authorization Code + PKCE 流程中的用户认证 UI）。
 * <p>
 * admin 与 portal 使用不同模板，表单统一 POST 到 {@code /login/authenticate}。
 *
 * @author Fu Wei
 */
@Controller
@RequiredArgsConstructor
public class LoginPageController {

    /** Auth 服务内部登录表单处理 URL（网关 StripPrefix 后匹配此路径） */
    public static final String LOGIN_PROCESSING_URL = "/login/authenticate";

    /** 登录页动态配置（系统名称 / Logo / 版权 / 轮播图） */
    private final AuthLoginProperties authLoginProperties;

    /** 已登录用户从登录页恢复到 authorize SavedRequest */
    private final OAuthLoginRedirectResolver redirectResolver;

    /** OAuth2 客户端配置（用于已登录但无 pending authorize 时自动跳转 SPA） */
    private final OAuthClientsProperties oauthClientsProperties;

    /**
     * 管理后台登录页（简单占位 UI，后续可按产品需求替换样式）。
     *
     * @param error  Spring Security 登录失败时携带的错误标记
     * @param model  视图模型
     * @return Thymeleaf 模板路径
     */
    @GetMapping("/login/admin")
    public String adminLogin(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "captcha-error", required = false) String captchaError,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        // 已登录且存在 authorize SavedRequest 时，直接继续 OAuth2 授权（避免停留在登录页）
        String resumeAuthorize = resolveResumeAuthorizeUrl(request, response);
        if (resumeAuthorize != null) {
            return "redirect:" + resumeAuthorize;
        }
        // 页面标题用于模板展示
        model.addAttribute("pageTitle", authLoginProperties.getSystemName() + " 管理后台");
        // 登录渠道 hidden 字段值，供 LoginChannelFilter 识别
        model.addAttribute("loginChannel", LoginChannel.ADMIN.getValue());
        // 表单提交地址（通过配置指定，直连为 /login/authenticate，网关模式为 /auth/login/authenticate）
        model.addAttribute("loginProcessingUrl", authLoginProperties.getLoginProcessingUrl());
        // 是否展示认证错误提示
        model.addAttribute("loginError", error != null);
        // 是否展示验证码错误提示
        model.addAttribute("captchaError", captchaError != null);
        // 验证码唯一标识（前端用此 key 请求图片）
        model.addAttribute("captchaKey", UUID.randomUUID().toString());
        // 系统动态配置
        model.addAttribute("systemName", authLoginProperties.getSystemName());
        model.addAttribute("logoUrl", authLoginProperties.getLogoUrl());
        model.addAttribute("copyright", authLoginProperties.getCopyright());
        model.addAttribute("carouselImages", authLoginProperties.getCarouselImages());
        // 返回 admin 专用模板
        return "login/admin";
    }

    /**
     * 前台门户登录页（简单占位 UI，与管理后台区分配色与文案）。
     *
     * @param error  Spring Security 登录失败时携带的错误标记
     * @param model  视图模型
     * @return Thymeleaf 模板路径
     */
    @GetMapping("/login/portal")
    public String portalLogin(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "captcha-error", required = false) String captchaError,
            Model model, HttpServletRequest request, HttpServletResponse response) {
        // 已登录且存在 authorize SavedRequest 时，直接继续 OAuth2 授权
        String resumeAuthorize = resolveResumeAuthorizeUrl(request, response);
        if (resumeAuthorize != null) {
            return "redirect:" + resumeAuthorize;
        }
        // 门户页标题
        model.addAttribute("pageTitle", authLoginProperties.getSystemName() + " 前台门户");
        // portal 渠道标识
        model.addAttribute("loginChannel", LoginChannel.PORTAL.getValue());
        // 表单提交地址（通过配置指定，直连为 /login/authenticate，网关模式为 /auth/login/authenticate）
        model.addAttribute("loginProcessingUrl", authLoginProperties.getLoginProcessingUrl());
        // 登录失败标记
        model.addAttribute("loginError", error != null);
        // 是否展示验证码错误提示
        model.addAttribute("captchaError", captchaError != null);
        // 验证码唯一标识（前端用此 key 请求图片）
        model.addAttribute("captchaKey", UUID.randomUUID().toString());
        // 系统动态配置
        model.addAttribute("systemName", authLoginProperties.getSystemName());
        model.addAttribute("logoUrl", authLoginProperties.getLogoUrl());
        model.addAttribute("copyright", authLoginProperties.getCopyright());
        model.addAttribute("carouselImages", authLoginProperties.getCarouselImages());
        // 返回 portal 专用模板
        return "login/portal";
    }

    /**
     * 若当前用户已认证，则优先恢复 pending authorize；无可恢复请求时自动跳转 SPA，由路由守卫发起 PKCE。
     */
    private String resolveResumeAuthorizeUrl(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        // 已认证但渠道不匹配时必须强制退出，阻断 admin 与 portal 的静默串登。
        if (!isAuthenticatedChannelMatched(request)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            return null;
        }
        // 从 Session 中获取当前渠道（如果已登录）
        String channel = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            channel = (String) session.getAttribute(AuthSessionAttributes.LOGIN_CHANNEL);
        }
        String target = redirectResolver.resolvePostLoginRedirectUrl(request, response, channel);
        if (StringUtils.hasText(target)) {
            return target;
        }
        // 已登录但无可恢复的 authorize（如新开 Tab 直接访问登录页）：
        // 跳转到 SPA 根路径，SPA 路由守卫检测无本地 token 后自动发起 PKCE → authorize 流程。
        // 此时 Auth 已有有效 Session，authorize 直接通过，用户无缝进入系统。
        return buildSpaAutoRedirectUrl(request);
    }

    /** 从 OAuth2 redirect_uri 提取 SPA 根路径，通过 {@link OAuthClientsProperties#extractSpaRootUrl} 正确处理 admin / portal 的不同 base path。 */
    private String buildSpaAutoRedirectUrl(HttpServletRequest request) {
        boolean isPortal = request.getRequestURI().endsWith("/portal");
        String redirectUri = isPortal
                ? oauthClientsProperties.getPortal().getRedirectUri()
                : oauthClientsProperties.getAdmin().getRedirectUri();
        return oauthClientsProperties.extractSpaRootUrl(redirectUri);
    }

    /** 校验当前会话登录渠道是否与当前登录页一致。 */
    private boolean isAuthenticatedChannelMatched(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object value = session.getAttribute(AuthSessionAttributes.LOGIN_CHANNEL);
        if (!(value instanceof String channel) || !StringUtils.hasText(channel)) {
            return false;
        }
        LoginChannel expected = request.getRequestURI().endsWith("/portal") ? LoginChannel.PORTAL : LoginChannel.ADMIN;
        return expected.getValue().equals(channel);
    }
}
