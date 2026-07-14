package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.user.UserProfileResponse;
import io.github.fushuwei.scaskeleton.system.api.response.user.UserResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysUser;
import org.mapstruct.Mapper;

/**
 * 用户对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * 用户实体 → 用户详情响应（排除敏感字段）
     *
     * @param user 用户实体
     * @return 用户详情响应对象
     */
    UserResponse toUserResponse(SysUser user);

    /**
     * 用户实体 → 当前登录用户资料响应（仅 id、username、nickname）
     *
     * @param user 用户实体
     * @return 当前登录用户基本信息响应对象
     */
    UserProfileResponse toUserProfileResponse(SysUser user);
}
