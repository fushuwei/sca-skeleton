package io.github.fushuwei.scaskeleton.system.api.request.user;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建用户请求对象
 *
 * @author Fu Wei
 */
@Data
public class UserCreateRequest {

    // ==================== 基本信息 ====================

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度 2-50 个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /** 初始密码 */
    @Size(min = 8, max = 20, message = "密码长度 8-20 个字符")
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

    /** 用户状态 */
    @Pattern(regexp = "^(active|inactive|locked|frozen|expired|disabled|cancelled)$", message = "状态值不合法")
    private String status;

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
