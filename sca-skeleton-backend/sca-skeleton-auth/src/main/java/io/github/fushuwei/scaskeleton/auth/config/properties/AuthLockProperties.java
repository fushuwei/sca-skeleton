package io.github.fushuwei.scaskeleton.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 登录失败锁定策略配置（对应 {@code sca.auth.lock.*}）。
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.auth.lock")
public class AuthLockProperties {

    /** 连续失败达到该次数后锁定账号 */
    private int maxFailCount = 5;

    /** 锁定持续时间（秒），到期后允许自动解锁 */
    private long lockDurationSeconds = 1800;
}
