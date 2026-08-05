package io.github.fushuwei.scaskeleton.datasource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源实体类
 * <p>
 * 按租户隔离。v1 不提供 url_override 字段（已删除，拼接 URL 覆盖 99% 场景）。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_datasource")
public class Datasource extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 数据源名称 */
    private String name;

    /** 数据库类型枚举 */
    private String dbType;

    /** 驱动ID */
    private String driverId;

    /** 数据库主机地址 */
    private String host;

    /** 数据库端口 */
    private Integer port;

    /** 数据库名/Schema/实例名 */
    private String databaseName;

    /** 数据库用户名 */
    private String username;

    /** 数据库密码（AES-GCM 加密存储） */
    private String password;

    /** 密钥版本号 */
    private String cipherVersion;

    /** JDBC 连接参数（JSON，仅白名单内 key） */
    private String connectionParams;

    /** 连接池配置（JSON） */
    private String poolConfig;

    /** 是否启用（0否 1是） */
    private Integer isEnabled;

    /** 状态（normal 正常、offline 离线、error 异常） */
    private String status;

    /** 最近连接错误信息 */
    private String errorMsg;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
