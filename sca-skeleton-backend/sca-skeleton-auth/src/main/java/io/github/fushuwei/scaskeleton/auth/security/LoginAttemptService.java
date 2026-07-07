package io.github.fushuwei.scaskeleton.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.fushuwei.scaskeleton.auth.config.properties.AuthLockProperties;
import io.github.fushuwei.scaskeleton.auth.infrastructure.entity.SysUser;
import io.github.fushuwei.scaskeleton.auth.infrastructure.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录失败计数与账号锁定服务。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /** 用户数据访问 */
    private final SysUserMapper sysUserMapper;

    /** 锁定策略配置 */
    private final AuthLockProperties lockProperties;

    /**
     * 登录成功后清零连续失败次数。
     *
     * @param username 登录用户名
     * @param channel  登录渠道
     */
    public void onLoginSuccess(String username, LoginChannel channel) {
        // 按渠道定位用户记录
        SysUser user = findUser(username, channel);
        if (user == null) {
            return;
        }
        // 仅重置失败计数，不改动其它状态字段
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLoginFailCount(0);
        sysUserMapper.updateById(update);
    }

    /**
     * 登录失败后递增计数，达到阈值时锁定账号。
     *
     * @param username 登录用户名
     * @param channel  登录渠道
     */
    public void onLoginFailure(String username, LoginChannel channel) {
        // 按渠道定位用户记录
        SysUser user = findUser(username, channel);
        if (user == null) {
            return;
        }
        // 累加连续失败次数
        int previousCount = user.getLoginFailCount() == null ? 0 : user.getLoginFailCount();
        int failCount = previousCount + 1;
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLoginFailCount(failCount);
        // 达到阈值后写入 locked 状态与原因
        if (failCount >= lockProperties.getMaxFailCount()) {
            update.setStatus("locked");
            update.setStatusTime(LocalDateTime.now());
            update.setStatusReason("连续登录失败超过" + lockProperties.getMaxFailCount() + "次");
            log.warn("[Auth] account locked due to repeated failures. username={} failCount={}",
                    username, failCount);
        }
        sysUserMapper.updateById(update);
    }

    /**
     * 锁定到期后自动解锁（仅处理因连续失败触发的 locked 状态）。
     *
     * @param user 待检查的用户实体（会被就地更新内存字段）
     */
    public void unlockIfExpired(SysUser user) {
        // 非 locked 或无锁定时间则跳过
        if (!"locked".equals(user.getStatus()) || user.getStatusTime() == null) {
            return;
        }
        // 仍在锁定期内则保持 locked
        LocalDateTime unlockAt = user.getStatusTime().plusSeconds(lockProperties.getLockDurationSeconds());
        if (unlockAt.isAfter(LocalDateTime.now())) {
            return;
        }
        // 锁定期已过，恢复 active 并清零失败计数
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setStatus("active");
        update.setLoginFailCount(0);
        update.setStatusTime(null);
        update.setStatusReason(null);
        sysUserMapper.updateById(update);
        user.setStatus("active");
        user.setLoginFailCount(0);
        user.setStatusTime(null);
        user.setStatusReason(null);
        log.info("[Auth] account auto-unlocked after lock duration. username={}", user.getUsername());
    }

    /**
     * 按用户名与登录渠道查询用户。
     */
    private SysUser findUser(String username, LoginChannel channel) {
        // portal 渠道查 frontend 用户，其余查 backend 用户
        String userType = LoginChannel.PORTAL == channel ? "frontend" : "backend";
        return sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getUserType, userType)
        );
    }
}
