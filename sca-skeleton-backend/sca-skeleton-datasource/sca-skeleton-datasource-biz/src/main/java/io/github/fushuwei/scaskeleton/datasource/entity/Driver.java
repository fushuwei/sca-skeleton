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

    /** JDBC Driver 全限定类名（MongoDB 等非 JDBC 类型可为空） */
    private String driverClass;

    /** 驱动目录键（{driverName}，目录下存放驱动文件 + 依赖） */
    private String objectKey;

    /** 存储类型（local/minio） */
    private String storageType;

    /** JDBC URL 前缀模板 */
    private String urlTemplate;

    /** URL 参数白名单（JSON） */
    private String allowedParams;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
