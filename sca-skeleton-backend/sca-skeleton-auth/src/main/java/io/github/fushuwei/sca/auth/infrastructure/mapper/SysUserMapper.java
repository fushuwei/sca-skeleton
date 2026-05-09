package io.github.fushuwei.sca.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.fushuwei.sca.auth.infrastructure.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * sys_user 数据访问。
 *
 * @author Fu Wei
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
