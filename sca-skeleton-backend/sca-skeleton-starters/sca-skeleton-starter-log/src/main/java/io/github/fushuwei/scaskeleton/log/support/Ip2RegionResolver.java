package io.github.fushuwei.scaskeleton.log.support;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;

/**
 * 基于 ip2region 离线 xdb 库的 IP 地理位置解析器
 *
 * @author Fu Wei
 */
@Slf4j
public class Ip2RegionResolver implements IpRegionResolver, AutoCloseable {

    private final Ip2Region ip2Region;

    private Ip2RegionResolver(Ip2Region ip2Region) {
        this.ip2Region = ip2Region;
    }

    /**
     * 从指定路径加载 xdb 文件创建解析器实例
     *
     * @param dbPath xdb 文件路径
     * @return 解析器实例
     * @throws Exception xdb 文件加载失败时抛出
     */
    public static Ip2RegionResolver create(String dbPath) throws Exception {
        Config v4Config = Config.custom()
            .setCachePolicy(Config.BufferCache)
            .setXdbPath(dbPath)
            .asV4();
        Ip2Region ip2Region = Ip2Region.create(v4Config, null);
        return new Ip2RegionResolver(ip2Region);
    }

    @Override
    public String resolve(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            String region = ip2Region.search(ip);
            return formatRegion(region);
        } catch (Exception e) {
            log.warn("[IP解析] 无法解析 IP: {} ({})", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 格式化 ip2region 返回的区域字符串
     * <p>
     * 原始格式: {@code 国家|区域|省份|城市|ISP}
     * <ul>
     *   <li>{@code 中国|0|北京|北京市|电信} → {@code 中国 北京 北京市 电信}</li>
     *   <li>{@code 0|0|0|0|内网IP} → {@code 内网IP}</li>
     *   <li>{@code 0|0|0|0|0} → {@code null}</li>
     * </ul>
     */
    private String formatRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        String[] parts = region.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!"0".equals(part) && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(part);
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    @Override
    public void close() {
        try {
            ip2Region.close();
        } catch (Exception e) {
            log.warn("[IP解析] 关闭 Ip2Region 服务失败", e);
        }
    }
}
