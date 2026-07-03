package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据实体。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    private String dictId;
    private String label;
    private String value;
    /** 值类型：string / int / boolean */
    private String valueType;
    /** Tag 颜色类型：default / primary / success / warning / danger / info */
    private String colorType;
    private String cssClass;
    private Integer status;
    private Integer isDefault;
    private Integer sort;
    private String remark;
}
