package io.github.fushuwei.scaskeleton.core.support;

/**
 * IP 地理位置解析器
 * <p>
 * 将客户端 IP 解析为可读的地理位置字符串（如 "中国 北京 北京市 电信"）
 *
 * @author Fu Wei
 */
public interface IpRegionResolver {

    /**
     * 解析 IP 地址对应的地理位置
     *
     * @param ip 客户端 IP 地址
     * @return 地理位置字符串，解析失败或无法识别时返回 {@code null}
     */
    String resolve(String ip);
}
