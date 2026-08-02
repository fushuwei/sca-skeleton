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
public class DsDriver extends BaseEntity {

    /** 数据库类型 */
    private String dbType;

    /** 驱动名称 */
    private String driverName;

    /** JDBC Driver 全限定类名 */
    private String driverClass;

    /** 驱动业务版本（如 8.0.33） */
    private String driverVersion;

    /** JAR 文件 SHA256 校验值 */
    private String jarSha256;

    /** 存储对象键（内容寻址：driver/{dbType}/{sha256}/{name}.jar） */
    private String objectKey;

    /** JAR 文件大小（字节） */
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
