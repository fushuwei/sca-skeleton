package io.github.fushuwei.scaskeleton.system.api.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新用户请求对象
 *
 * @author Fu Wei
 */
@Data
public class UserUpdateRequest {

    // ==================== 基本信息 ====================

    /** 用户 ID */
    @NotBlank(message = "用户 ID 不能为空")
    private String id;

    /** 新密码（留空表示不修改） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 真实姓名 */
    private String realName;

    /** 性别 */
    @Pattern(regexp = "^(male|female|other)$", message = "性别只能是 male、female 或 other")
    private String gender;

    /** 手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    // ==================== 属性信息 ====================

    /** 是否平台超级管理员 */
    private Integer isSuperadmin;

    /** 是否必须修改密码 */
    private Integer mustChangePassword;

    /** 账号生效起始时间 */
    private LocalDateTime effectiveStartTime;

    /** 账号生效截止时间 */
    private LocalDateTime effectiveEndTime;

    /** 备注 */
    private String remark;

    // ==================== 关联信息 ====================

    /** 部门 ID 列表 */
    private List<String> deptIds;

    /** 岗位 ID 列表 */
    private List<String> postIds;

    /** 角色 ID 列表 */
    private List<String> roleIds;
}
