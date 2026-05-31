package io.github.fushuwei.scaskeleton.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Auth 服务登录页动态配置属性（{@code sca.auth.login.*}）。
 * <p>
 * 通过 Thymeleaf 注入到 admin / portal 登录页模板，用于系统名称、Logo、版权信息
 * 及轮播图等可配置内容。未来可扩展为从数据库加载。
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.auth.login")
public class AuthLoginProperties {

    /** 系统名称（显示在登录页标题与页面中） */
    private String systemName = "SCA Skeleton";

    /** Logo 图片 URL（空字符串时使用默认 Material Icon 占位） */
    private String logoUrl = "";

    /** 页脚版权信息 */
    private String copyright = "© 2025 SCA Skeleton. All rights reserved.";

    /** 登录页轮播图 URL 列表（全屏背景） */
    private List<String> carouselImages = new ArrayList<>();

    /**
     * 登录表单提交 URL。
     * <p>
     * 直连模式：{@code /login/authenticate}（auth 服务内部路径）
     * 网关模式：{@code /auth/login/authenticate}（浏览器经网关 POST，网关 StripPrefix 后到达 auth 服务）
     */
    private String loginProcessingUrl = "/login/authenticate";
}
