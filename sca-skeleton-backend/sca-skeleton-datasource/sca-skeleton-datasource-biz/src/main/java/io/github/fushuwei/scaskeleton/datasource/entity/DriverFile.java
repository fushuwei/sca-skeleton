package io.github.fushuwei.scaskeleton.datasource.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 驱动文件实体类
 * <p>
 * 一个驱动可包含多个文件（主驱动 JAR + 依赖 JAR），每个文件单独记录元数据。
 * 通过 driver_id 关联到 ds_driver 表。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ds_driver_file")
public class DriverFile extends BaseEntity {

    /** 关联驱动ID */
    private String driverId;

    /** 文件名（上传时的原始文件名） */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件 SHA256 校验值 */
    private String sha256;

    /** 探测到的驱动类（逗号分隔，如 com.mysql.cj.jdbc.Driver,org.example.AnotherDriver） */
    private String driverClasses;

    /** 排序序号（主文件 0，依赖按上传顺序递增） */
    private Integer sortOrder;
}
