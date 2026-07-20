package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.Reference;
import io.github.fushuwei.scaskeleton.mybatis.reference.annotation.ReferencedBy;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位实体类
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
@ReferencedBy(
    value = {
        @Reference(table = "sys_user_post", column = "post_id", message = "岗位下存在用户，无法删除")
    },
    displayColumn = "name"
)
public class SysPost extends BaseEntity {

    /** 租户 ID */
    private String tenantId;

    /** 岗位名称 */
    private String name;

    /** 岗位编码 */
    private String code;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}
