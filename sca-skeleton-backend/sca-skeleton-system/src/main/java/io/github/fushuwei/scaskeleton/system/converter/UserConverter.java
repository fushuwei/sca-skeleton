package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysUser;
import org.mapstruct.Mapper;

/**
 * 用户 Entity → Response 转换器。
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /** SysUser → UserResponse（排除密码等敏感字段，字段名相同自动映射） */
    UserResponse toUserResponse(SysUser user);

    /** SysUser → UserProfileResponse（当前登录用户资料，仅 id、username、nickname） */
    UserProfileResponse toUserProfileResponse(SysUser user);
}
