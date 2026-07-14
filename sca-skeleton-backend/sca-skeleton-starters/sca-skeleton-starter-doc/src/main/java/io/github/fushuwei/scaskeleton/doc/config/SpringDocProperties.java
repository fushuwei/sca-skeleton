package io.github.fushuwei.scaskeleton.doc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Doc 文档配置属性类
 *
 * @author Fu Wei
 */
@Data
@ConfigurationProperties(prefix = "springdoc.api-docs.info")
public class SpringDocProperties {

    /**
     * 文档标题
     */
    private String title = "API Documentation";

    /**
     * 文档描述
     */
    private String description = "RESTful API Documentation";

    /**
     * 版本号
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
     * 联系人信息
     */
    @Data
    public static class Contact {

        /**
         * 联系人姓名
         */
        private String name = "";

        /**
         * 联系人邮箱
         */
        private String email = "";

        /**
         * 联系人网址
         */
        private String url = "";
    }

    /**
     * 许可证信息
     */
    @Data
    public static class License {

        /**
         * 许可证名称
         */
        private String name = "";

        /**
         * 许可证网址
         */
        private String url = "";
    }
}
