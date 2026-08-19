package io.github.fushuwei.scaskeleton.system.api.request.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新系统配置请求对象
 *
 * @author Fu Wei
 */
@Data
public class ConfigUpdateRequest {

    /** 配置 ID */
    @NotBlank(message = "配置 ID 不能为空")
    private String id;

    /** 配置名称 */
    @NotBlank(message = "配置名称不能为空")
    private String name;

    /** 配置键 */
    @NotBlank(message = "配置键不能为空")
    private String key;

    /** 配置值 */
    private String value;

    /** 类型（string/number/boolean/datetime/json） */
    @NotBlank(message = "配置类型不能为空")
    private String type;

    /** 状态（enabled 启用，disabled 禁用） */
    @NotBlank(message = "配置状态不能为空")
    private String status;

    /** 备注 */
    private String remark;

    // ==================== 乐观锁 ====================

    /** 乐观锁版本号 */
    private Integer version;
}
