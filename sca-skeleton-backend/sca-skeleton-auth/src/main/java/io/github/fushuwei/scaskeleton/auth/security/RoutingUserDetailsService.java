package io.github.fushuwei.scaskeleton.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 路由式用户详情服务：根据 {@link LoginChannelContext} 将认证请求分发到不同用户类别加载逻辑。
 * <p>
 * admin 渠道加载 {@code user_category=backend}；portal 渠道加载 {@code user_category=frontend}。
 *
 * @author Fu Wei
 */
@Service
@RequiredArgsConstructor
public class RoutingUserDetailsService implements UserDetailsService {

    /** 实际的用户查询与权限组装服务 */
    private final ScaUserDetailsService scaUserDetailsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 读取当前登录渠道（Filter 已写入 ThreadLocal）
        LoginChannel channel = LoginChannelContext.get();
        if (channel == null) {
            throw new UsernameNotFoundException("Login channel missing");
        }
        // portal 走前台用户表过滤条件
        if (LoginChannel.PORTAL == channel) {
            return scaUserDetailsService.loadFrontendUserByUsername(username);
        }
        // admin：后台用户
        return scaUserDetailsService.loadUserByUsername(username);
    }
}
