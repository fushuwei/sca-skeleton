package com.itangsoft.skeleton.starter.core.autoconfigure;

import com.itangsoft.skeleton.starter.core.runner.BannerApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Banner自动配置类
 *
 * @author fushuwei
 */
@Configuration
public class BannerAutoConfiguration {

    @Bean
    public BannerApplicationRunner bannerApplicationRunner() {
        return new BannerApplicationRunner();
    }
}
