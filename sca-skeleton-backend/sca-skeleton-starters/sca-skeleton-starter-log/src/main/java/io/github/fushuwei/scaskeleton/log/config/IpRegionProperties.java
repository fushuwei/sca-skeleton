package io.github.fushuwei.scaskeleton.log.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IP 地理位置解析配置属性类
 * <pre>
 * sca:
 *   ip-region:
 *     db-path: ./ip2region/ip2region_v4.xdb
 * </pre>
 *
 * @author Fu Wei
 */
@Getter
@ConfigurationProperties(prefix = "sca.ip-region")
public class IpRegionProperties {

    /**
     * ip2region xdb 数据库文件路径
     * <p>
     * xdb 文件下载地址: <a href="https://github.com/lionsoul2014/ip2region/blob/master/data/ip2region_v4.xdb">ip2region_v4.xdb</a>
     */
    @Setter
    private String dbPath = "./ip2region/ip2region_v4.xdb";
}
