package io.github.fushuwei.scaskeleton.core.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页响应体
 * 为分页查询接口提供统一的响应格式，包含分页信息和数据列表
 *
 * @author Fu Wei
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从1开始）
     */
    private long current;

    /**
     * 每页显示数量
     */
    private long size;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 当前页数据列表
     */
    private List<T> records;

    /**
     * 构造函数
     */
    public PageResult() {
    }

    /**
     * 构造函数
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param total   总记录数
     * @param records 数据列表
     */
    public PageResult(long current, long size, long total, List<T> records) {
        this.current = current;
        this.size = size;
        this.total = total;
        this.records = records;
        this.pages = (total + size - 1) / size; // 计算总页数
    }

    /**
     * 创建空的分页结果
     *
     * @param <T> 数据类型
     * @return PageResult<T>
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(1, 10, 0, List.of());
    }

    /**
     * 创建分页结果
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param total   总记录数
     * @param records 数据列表
     * @param <T>     数据类型
     * @return PageResult<T>
     */
    public static <T> PageResult<T> of(long current, long size, long total, List<T> records) {
        return new PageResult<>(current, size, total, records);
    }

    /**
     * 判断是否为空页
     *
     * @return boolean
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }

    /**
     * 判断是否为第一页
     *
     * @return boolean
     */
    public boolean isFirst() {
        return current <= 1;
    }

    /**
     * 判断是否为最后一页
     *
     * @return boolean
     */
    public boolean isLast() {
        return current >= pages;
    }

    /**
     * 获取偏移量
     *
     * @return long 偏移量
     */
    public long getOffset() {
        return (current - 1) * size;
    }
}
