package io.github.fushuwei.scaskeleton.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.fushuwei.scaskeleton.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户岗位关联。
 *
 * @author Fu Wei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_post")
public class SysUserPost extends BaseEntity {

    private String tenantId;
    private String userId;
    private String postId;
}
