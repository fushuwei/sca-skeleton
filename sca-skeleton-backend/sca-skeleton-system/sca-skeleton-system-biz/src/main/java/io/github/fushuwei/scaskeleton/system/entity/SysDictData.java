package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    // ==================== 基本信息 ====================

    /** 租户 ID */
    private String tenantId;

    /** 字典 ID */
    private String dictId;

    /** 字典标签 */
    private String label;

    /** 字典值 */
    private String value;

    // ==================== 状态与排序 ====================

    /** 状态 */
    private String status;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;
}
