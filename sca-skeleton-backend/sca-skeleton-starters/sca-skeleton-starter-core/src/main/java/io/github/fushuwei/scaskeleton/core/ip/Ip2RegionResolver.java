package io.github.fushuwei.scaskeleton.core.ip;

import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;

import java.net.InetAddress;

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
        // 内网/回环地址直接返回，避免 ip2region 返回无意义的 "Reserved" 段
        if (isInternalIp(ip)) {
            return "内网IP";
        }
        try {
            String region = ip2Region.search(ip);
            String formatted = formatRegion(region);
            log.info("[IP解析] clientIp={}, 原始数据={}, 位置={}", ip, region, formatted);
            return formatted;
        } catch (Exception e) {
            log.warn("[IP解析] 无法解析 IP: {} ({})", ip, e.getMessage());
            return null;
        }
    }

    /**
     * 判断是否为内网/回环/链路本地等非公网地址
     * <p>
     * 覆盖：127.0.0.0/8（回环）、10.0.0.0/8、172.16.0.0/12、192.168.0.0/16（站点本地）、169.254.0.0/16（链路本地）、0.0.0.0（任意本地）
     */
    private boolean isInternalIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isLoopbackAddress()
                || address.isAnyLocalAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 格式化 ip2region 返回的区域字符串
     * <p>
     * 原始格式: {@code 国家|省份|城市|ISP|国家代码}
     * <p>
     * 只保留国家/省份/城市（索引 0~2），剔除运营商和国家代码。
     * <ul>
     *   <li>{@code 中国|湖北省|武汉市|移动|CN} → {@code 中国 湖北省 武汉市}</li>
     *   <li>{@code 中国|湖北省|0|移动|CN} → {@code 中国 湖北省}</li>
     *   <li>{@code 0|0|0|0|0} → {@code null}</li>
     * </ul>
     */
    private String formatRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        String[] parts = region.split("\\|");
        // 只保留索引 0~2（国家、省份、城市），剔除运营商(3)和国家代码(4)
        int start = 0;
        int end = Math.min(parts.length, 3);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            String part = parts[i];
            // 过滤 {@code "0"}、空白、{@code "Reserved"}（ip2region 对保留/未分配 IP 段返回的无意义占位符）
            if (!"0".equals(part) && !part.isBlank() && !"Reserved".equals(part)) {
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
