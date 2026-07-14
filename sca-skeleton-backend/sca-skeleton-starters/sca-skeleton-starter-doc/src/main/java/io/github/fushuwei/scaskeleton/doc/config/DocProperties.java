package io.github.fushuwei.scaskeleton.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API 文档配置属性类
 * <p>
 * 用于配置 OpenAPI 文档的元数据信息（标题、描述、版本、联系人、许可证等）
 * SpringDoc 原生的 {@code springdoc.*} 属性仍可用于控制文档路径、扫描包等技术参数
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "sca.doc")
public class DocProperties {

    /**
     * 是否启用 API 文档，默认开启
     * <p>
     * 生产环境可通过 {@code sca.doc.enabled=false} 关闭文档暴露
     */
    private boolean enabled = true;

    /**
     * API 文档标题
     */
    private String title = "API Documentation";

    /**
     * API 文档描述
     */
    private String description = "RESTful API Documentation";

    /**
     * API 版本号
     */
    private String version = "1.0.0";

    /**
     * 联系人信息
     */
    private Contact contact = new Contact();

    /**
     * 许可证信息
     */
    private License license = new License();

    /**
     * API 联系人信息
     */
    @Data
    public static class Contact {

        /** 联系人姓名 */
        private String name = "";

        /** 联系人邮箱 */
        private String email = "";

        /** 联系人网址 */
        private String url = "";
    }

    /**
     * API 许可证信息
     */
    @Data
    public static class License {

        /** 许可证名称 */
        private String name = "";

        /** 许可证网址 */
        private String url = "";
    }
}
