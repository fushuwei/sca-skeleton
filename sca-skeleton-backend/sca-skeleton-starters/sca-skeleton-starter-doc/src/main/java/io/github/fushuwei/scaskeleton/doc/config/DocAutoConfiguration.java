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
 * API 文档自动配置类
 * <p>
 * 基于 SpringDoc OpenAPI 3 + Swagger UI，为微服务提供开箱即用的 API 文档能力
 * 各微服务只需引入本 Starter 依赖，即可通过 {@code sca.doc.*} 属性自定义文档元数据，
 * 并通过 SpringDoc 原生 {@code springdoc.*} 属性控制路径、扫描包等技术参数
 * <p>
 * 内置 OAuth2 Bearer Token 安全方案，在文档 UI 中点击「Authorize」输入 Access Token 后，
 * 后续请求自动携带 {@code Authorization: Bearer <token>} 请求头，便于直接调试受保护接口。
 * <p>
 * 访问地址：{@code http://ip:port/swagger-ui.html} 或 {@code http://ip:port/v3/api-docs}
 *
 * @author Fu Wei
 */
@AutoConfiguration
@EnableConfigurationProperties(DocProperties.class)
@ConditionalOnProperty(prefix = "sca.doc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocAutoConfiguration {

    /** Bearer Token 安全方案名称，用于关联 SecurityRequirement 与 SecurityScheme */
    private static final String SECURITY_SCHEME_NAME = "Bearer Token";

    /**
     * 注册 OpenAPI 文档元数据 Bean
     * <p>
     * 包含文档基本信息（标题、描述、版本、联系人、许可证）与 OAuth2 Bearer Token 安全方案。
     * 当容器中不存在 {@link OpenAPI} Bean 时生效，允许微服务自定义覆盖
     *
     * @param properties 文档配置属性
     * @return OpenAPI 实例
     */
    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI openAPI(DocProperties properties) {
        DocProperties.Contact contact = properties.getContact();
        DocProperties.License license = properties.getLicense();

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
                    .description("输入 OAuth2 Access Token（不透明令牌），请求头将自动携带 Authorization: Bearer <token>")))
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
