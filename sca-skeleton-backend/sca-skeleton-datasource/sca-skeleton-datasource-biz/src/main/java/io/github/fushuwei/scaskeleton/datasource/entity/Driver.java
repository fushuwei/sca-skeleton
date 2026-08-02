package io.github.fushuwei.scaskeleton.datasource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.Reference;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.ReferencedBy;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 驱动实体类
 * <p>
 * 系统级全局资产（无 tenant_id）。存储驱动 JAR 的元数据与加载信息。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_driver")
@ReferencedBy({
    @Reference(table = "ds_datasource", column = "driver_id", message = "驱动被数据源引用，无法删除")
})
public class Driver extends BaseEntity {

    /** 数据库类型 */
    private String dbType;

    /** 驱动名称 */
    private String driverName;

    /** JDBC Driver 全限定类名 */
    private String driverClass;

    /** 驱动业务版本（如 8.0.33） */
    private String driverVersion;

    /** 主 JAR 文件 SHA256 校验值（用于展示，多 JAR 场景下仅记录首个主 JAR） */
    private String jarSha256;

    /** 驱动目录键（drivers/{driverName}，目录下存放驱动主 JAR + 所有依赖 JAR） */
    private String objectKey;

    /** 所有 JAR 文件总大小（字节） */
    private Long fileSize;

    /** 存储类型（local/minio） */
    private String storageType;

    /** JDBC URL 前缀模板 */
    private String urlTemplate;

    /** URL 参数白名单（JSON） */
    private String allowedParams;

    /** 状态（enabled/disabled） */
    private String status;

    /** 是否内置驱动（0 否，1 是） */
    private Integer isBuiltin;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
