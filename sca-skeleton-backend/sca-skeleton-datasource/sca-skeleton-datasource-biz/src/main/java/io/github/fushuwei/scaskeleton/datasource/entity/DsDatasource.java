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
public class DsDatasource extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 数据源名称 */
    private String name;

    /** 数据库类型枚举 */
    private String dbType;

    /** 关联驱动 ID（指向 ds_driver.id） */
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
    private String passwordCipher;

    /** 密钥版本号 */
    private String cipherVersion;

    /** JDBC 连接参数（JSON，仅白名单内 key） */
    private String connectionParams;

    /** 连接池配置（JSON） */
    private String poolConfig;

    /** 管理态（1 启用，0 禁用） */
    private Integer enabled;

    /** 运行态（offline/online/error） */
    private String connectionState;

    /** 最近连接错误信息 */
    private String errorMsg;

    /** 最后连接时间 */
    private java.time.LocalDateTime lastConnectTime;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
