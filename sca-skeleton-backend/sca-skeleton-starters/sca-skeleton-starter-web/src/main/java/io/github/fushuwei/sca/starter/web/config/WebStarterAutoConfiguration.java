package io.github.fushuwei.sca.starter.web.config;

import io.github.fushuwei.sca.starter.web.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Web 能力自动配置。
 *
 * @author Fu Wei
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebStarterAutoConfiguration {
}
