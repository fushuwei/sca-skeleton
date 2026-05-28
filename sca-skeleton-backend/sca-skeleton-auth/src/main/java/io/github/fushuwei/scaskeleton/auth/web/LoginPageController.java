package io.github.fushuwei.scaskeleton.auth.web;

import io.github.fushuwei.scaskeleton.auth.config.properties.OAuthClientsProperties;
import io.github.fushuwei.scaskeleton.auth.security.LoginChannel;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    /** OAuth2 客户端配置（含网关对外路径前缀） */
    private final OAuthClientsProperties oauthClientsProperties;

    /** 已登录用户从登录页恢复到 authorize SavedRequest */
    private final OAuthLoginRedirectResolver redirectResolver;

    /**
     * 管理后台登录页（简单占位 UI，后续可按产品需求替换样式）。
     *
     * @param error  Spring Security 登录失败时携带的错误标记
     * @param model  视图模型
     * @return Thymeleaf 模板路径
     */
    @GetMapping("/login/admin")
    public String adminLogin(@RequestParam(value = "error", required = false) String error, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        // 已登录且存在 authorize SavedRequest 时，直接继续 OAuth2 授权（避免停留在登录页）
        String resumeAuthorize = resolveResumeAuthorizeUrl(request, response);
        if (resumeAuthorize != null) {
            return "redirect:" + resumeAuthorize;
        }
        // 页面标题用于模板展示
        model.addAttribute("pageTitle", "SCA 管理后台登录");
        // 登录渠道 hidden 字段值，供 LoginChannelFilter 识别
        model.addAttribute("loginChannel", LoginChannel.ADMIN.getValue());
        // 表单提交地址（浏览器经网关 POST /auth/login/authenticate）
        model.addAttribute("loginProcessingUrl", oauthClientsProperties.getExternalLoginProcessingUrl());
        // 是否展示错误提示
        model.addAttribute("loginError", error != null);
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
    public String portalLogin(@RequestParam(value = "error", required = false) String error, Model model,
            HttpServletRequest request, HttpServletResponse response) {
        // 已登录且存在 authorize SavedRequest 时，直接继续 OAuth2 授权
        String resumeAuthorize = resolveResumeAuthorizeUrl(request, response);
        if (resumeAuthorize != null) {
            return "redirect:" + resumeAuthorize;
        }
        // 门户页标题
        model.addAttribute("pageTitle", "SCA 前台门户登录");
        // portal 渠道标识
        model.addAttribute("loginChannel", LoginChannel.PORTAL.getValue());
        // 表单提交地址（浏览器经网关 POST /auth/login/authenticate）
        model.addAttribute("loginProcessingUrl", oauthClientsProperties.getExternalLoginProcessingUrl());
        // 登录失败标记
        model.addAttribute("loginError", error != null);
        // 返回 portal 专用模板
        return "login/portal";
    }

    /**
     * 若当前用户已认证且 Session 中仍有 authorize SavedRequest，则返回应恢复的绝对 URL。
     */
    private String resolveResumeAuthorizeUrl(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String target = redirectResolver.resolvePostLoginRedirectUrl(request, response);
        return StringUtils.hasText(target) ? target : null;
    }
}
