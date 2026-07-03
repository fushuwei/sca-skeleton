package io.github.fushuwei.scaskeleton.remote.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 远程调用客户端配置属性。
 * <p>
 * 对应配置前缀 {@code sca.remote}，可在 {@code application.yml} 中按需调整：
 * <pre>
 * sca:
 *   remote:
 *     connect-timeout: 5000
 *     read-timeout: 30000
 * </pre>
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.remote")
public class RemoteClientProperties {

    /**
     * 连接超时时间（毫秒），默认 5 秒。
     * <p>
     * 指的是建立 TCP 连接的最大等待时间，超时后抛出 {@code ConnectException}。
     */
    private long connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒），默认 30 秒。
     * <p>
     * 指的是从连接建立到接收完整响应的最大等待时间，超时后抛出 {@code SocketTimeoutException}。
     * 对于慢接口可按需调大，但应避免无限等待以防止线程阻塞。
     */
    private long readTimeout = 30000;
}
