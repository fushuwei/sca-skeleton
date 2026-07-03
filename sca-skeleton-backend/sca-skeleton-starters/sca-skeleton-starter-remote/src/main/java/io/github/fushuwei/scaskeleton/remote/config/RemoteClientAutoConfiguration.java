package io.github.fushuwei.scaskeleton.remote.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.fushuwei.scaskeleton.remote.interceptor.RemoteHeaderInterceptor;
import io.github.fushuwei.scaskeleton.remote.interceptor.RemoteResponseInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 远程调用客户端自动配置入口。
 * <p>
 * 当 classpath 中存在 {@link RestClient} 时自动激活，注册以下组件：
 * <ul>
 *   <li>{@link RemoteHeaderInterceptor}：请求头传递拦截器（TraceId / Authorization）</li>
 *   <li>{@link RemoteResponseInterceptor}：统一响应解码拦截器（Result 解包 + 错误处理）</li>
 *   <li>{@link RestClient.Builder}：预配置的 RestClient 构建器（超时、连接池、拦截器），prototype 作用域</li>
 * </ul>
 * <p>
 * 通过 {@code @LoadBalanced} 标记 Builder，Spring Cloud LoadBalancer 会自动注入
 * {@code LoadBalancerInterceptor}，使 RestClient 能通过服务名（如 {@code http://sca-skeleton-system}）
 * 发起请求，由注册中心（Nacos）解析为实际实例地址。
 * <p>
 * 各微服务注入 {@link RestClient.Builder} 后，只需设置 baseUrl（服务名）即可创建 RestClient 实例，
 * 再通过 {@code HttpServiceProxyFactory} 注册 {@code @HttpExchange} 接口代理。
 * <p>
 * 使用 {@code @Primary} + {@code @Scope("prototype")} 确保每次注入均返回携带拦截器的新实例，
 * 避免多服务共享同一 Builder 导致 baseUrl 被覆盖。
 *
 * @author Fu Wei
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RemoteClientProperties.class)
public class RemoteClientAutoConfiguration {

    /**
     * 注册请求头传递拦截器，所有 RestClient 实例均自动生效。
     *
     * @return RemoteHeaderInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RemoteHeaderInterceptor remoteHeaderInterceptor() {
        return new RemoteHeaderInterceptor();
    }

    /**
     * 注册统一响应解码拦截器，自动解包 {@code Result<T>} 并处理错误。
     *
     * @param objectMapper Jackson ObjectMapper，由 Spring Boot 自动装配
     * @return RemoteResponseInterceptor 实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RemoteResponseInterceptor remoteResponseInterceptor(ObjectMapper objectMapper) {
        return new RemoteResponseInterceptor(objectMapper);
    }

    /**
     * 注册预配置的 RestClient.Builder。
     * <p>
     * 包含以下配置：
     * <ul>
     *   <li>连接超时 / 读取超时（来自 {@link RemoteClientProperties}）</li>
     *   <li>JDK HttpClient（支持 HTTP/2，内置连接池）</li>
     *   <li>请求头传递拦截器 + 统一响应解码拦截器</li>
     *   <li>LoadBalancerInterceptor（由 {@code @LoadBalanced} 自动注入，服务名 → 实例地址解析）</li>
     * </ul>
     * <p>
     * 使用 prototype 作用域，确保每次注入返回独立实例；
     * 使用 {@code @Primary} 覆盖 Spring Boot 默认 Builder，保证拦截器始终生效。
     * 各服务注入后，调用 {@code .baseUrl("http://service-name").build()} 即可创建专属 RestClient。
     *
     * @param properties          远程调用配置属性
     * @param headerInterceptor   请求头传递拦截器
     * @param responseInterceptor 统一响应解码拦截器
     * @return 预配置的 RestClient.Builder 实例
     */
    @Bean
    @LoadBalanced
    @Scope("prototype")
    @Primary
    public RestClient.Builder restClientBuilder(
            RemoteClientProperties properties,
            RemoteHeaderInterceptor headerInterceptor,
            RemoteResponseInterceptor responseInterceptor) {

        // 使用 JDK HttpClient 作为底层 HTTP 客户端，支持 HTTP/2 且内置连接池
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeout()));

        return RestClient.builder()
                .requestInterceptor(headerInterceptor)
                .requestInterceptor(responseInterceptor)
                .requestFactory(requestFactory);
    }
}
