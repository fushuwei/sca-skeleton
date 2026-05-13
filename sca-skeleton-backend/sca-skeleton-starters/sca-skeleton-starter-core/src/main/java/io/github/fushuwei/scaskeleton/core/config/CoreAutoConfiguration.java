package io.github.fushuwei.scaskeleton.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Core Starter 自动配置入口。
 * <p>
 * starter-core 主要提供纯 Java 工具类与接口契约，无需注册额外 Bean。
 * 此类作为自动配置占位，确保 {@code AutoConfiguration.imports} 声明有效，
 * 同时为后续扩展（如全局 ApplicationContext 注册）预留挂载点。
 *
 * @author Fu Wei
 */
@AutoConfiguration
public class CoreAutoConfiguration {
}
