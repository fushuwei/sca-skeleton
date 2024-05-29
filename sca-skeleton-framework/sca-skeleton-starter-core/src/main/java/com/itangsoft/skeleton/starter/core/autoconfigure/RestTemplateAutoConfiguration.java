package com.itangsoft.skeleton.starter.core.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 自动配置
 *
 * @author fushuwei
 */
public class RestTemplateAutoConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
