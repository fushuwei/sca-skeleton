package io.github.fushuwei.scaskeleton.datasource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SQL 查询历史实体类
 * <p>
 * 日志表，不继承 BaseEntity（只追加，不需逻辑删除/乐观锁）。
 * sql_content 截断长度 4000 字符（UTF-8）。
 *
 * @author Fu Wei
 */
@Data
@TableName("ds_query_log")
public class QueryLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 租户 ID */
    private String tenantId;

    /** 数据源 ID */
    private String datasourceId;

    /** 数据源名称（冗余，防删除后无法展示） */
    private String datasourceName;

    /** 数据库类型 */
    private String databaseType;

    /** 执行人 ID */
    private String userId;

    /** 执行人名称 */
    private String userName;

    /** 执行的 SQL 语句（超长截断 4000 字符） */
    private String sqlContent;

    /** SQL 类型 */
    private String sqlType;

    /** 执行状态（success/fail） */
    private String status;

    /** 执行耗时（毫秒） */
    private Long costMs;

    /** 影响/返回行数 */
    private Long rowCount;

    /** 错误信息 */
    private String errorMsg;

    /** 客户端 IP */
    private String clientIp;

    /** 执行时间 */
    private LocalDateTime createTime;
}
