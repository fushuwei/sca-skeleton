package io.github.fushuwei.scaskeleton.doc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring Doc 文档自动配置类
 *
 * @author Fu Wei
 */
@AutoConfiguration
@EnableConfigurationProperties(SpringDocProperties.class)
@ConditionalOnProperty(prefix = "springdoc.api-docs", name = "enabled", havingValue = "true")
public class SpringDocAutoConfiguration {

    /**
     * Bearer Token 安全方案名称，用于关联 SecurityRequirement 与 SecurityScheme
     */
    private static final String SECURITY_SCHEME_NAME = "Bearer Token";

    /**
     * 注册 OpenAPI 文档元数据 Bean
     *
     * @param properties 文档配置属性
     * @return OpenAPI 实例
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI openAPI(SpringDocProperties properties) {
        SpringDocProperties.Contact contact = properties.getContact();
        SpringDocProperties.License license = properties.getLicense();

        return new OpenAPI()
            .info(new Info()
                .title(properties.getTitle())
                .description(properties.getDescription())
                .version(properties.getVersion())
                .contact(new Contact()
                    .name(contact.getName())
                    .email(contact.getEmail())
                    .url(contact.getUrl()))
                .license(new License()
                    .name(license.getName())
                    .url(license.getUrl())))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .description("输入 Access Token（访问令牌），请求头将自动携带 Authorization: Bearer <token>")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
