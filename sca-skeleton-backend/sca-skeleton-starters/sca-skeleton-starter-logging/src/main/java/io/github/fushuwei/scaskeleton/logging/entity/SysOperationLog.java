package io.github.fushuwei.scaskeleton.logging.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体，映射 {@code sys_operation_log} 表。
 * <p>
 * 只增不改的审计记录，不继承 BaseEntity（无 createBy/updateBy/isDeleted/version 等字段）。
 * 操作人展示名称（operator）为非表字段，仅在 JOIN sys_user 查询时由 SQL 拼接填充。
 *
 * @author Fu Wei
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键：32 位小写无连字符 UUID v7，INSERT 时由自定义 IdentifierGenerator 自动生成 */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /** 链路追踪 ID */
    private String traceId;

    /** 操作人 ID（关联 sys_user.id） */
    private String userId;

    /** 操作人展示名称（非表字段，JOIN 时 SQL 拼接："real_name (username)"） */
    @TableField(value = "operator", insertStrategy = FieldStrategy.NEVER,
                updateStrategy = FieldStrategy.NEVER, select = false)
    private String operator;

    /** 操作模块 */
    private String module;

    /** 操作动作 */
    private String action;

    /** 请求方法（GET/POST 等） */
    private String httpMethod;

    /** 请求路径 */
    private String requestUri;

    /** 目标类全限定名 */
    private String className;

    /** 目标方法名 */
    private String methodName;

    /** 请求参数（JSON） */
    private String requestArgs;

    /** 响应结果（JSON） */
    private String responseResult;

    /** 是否成功：true-成功，false-异常 */
    private Boolean success;

    /** 异常信息 */
    private String errorMessage;

    /** 操作耗时（毫秒） */
    private Long costMs;

    /** 客户端 IP */
    private String clientIp;

    /** 操作时间 */
    private LocalDateTime operationTime;
}
