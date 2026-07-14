package io.github.fushuwei.scaskeleton.doc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * API 文档自动配置类
 * <p>
 * 基于 SpringDoc OpenAPI 3 + Scalar UI，为微服务提供开箱即用的 API 文档能力
 * 各微服务只需引入本 Starter 依赖，即可通过 {@code sca.doc.*} 属性自定义文档元数据，
 * 并通过 SpringDoc 原生 {@code springdoc.*} 属性控制路径、扫描包等技术参数
 * <p>
 * 访问地址：{@code http://ip:port/scalar} 或 {@code http://ip:port/swagger-ui.html}（取决于 SpringDoc 配置）
 *
 * @author Fu Wei
 */
@AutoConfiguration
@EnableConfigurationProperties(DocProperties.class)
@ConditionalOnProperty(prefix = "sca.doc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocAutoConfiguration {

    /**
     * 注册 OpenAPI 文档元数据 Bean
     * <p>
     * 当容器中不存在 {@link OpenAPI} Bean 时生效，允许微服务自定义覆盖
     *
     * @param properties 文档配置属性
     * @return OpenAPI 实例，包含标题、描述、版本、联系人、许可证等元数据
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
                    .url(license.getUrl())));
    }
}
