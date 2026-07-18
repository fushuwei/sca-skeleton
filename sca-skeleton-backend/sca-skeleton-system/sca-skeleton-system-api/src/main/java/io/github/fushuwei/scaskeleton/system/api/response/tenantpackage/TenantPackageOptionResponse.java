package io.github.fushuwei.scaskeleton.system.api.response.tenantpackage;

import lombok.Data;

/**
 * 租户套餐选项响应对象
 *
 * @author Fu Wei
 */
@Data
public class TenantPackageOptionResponse {

    /** 套餐 ID */
    private String id;

    /** 套餐名称 */
    private String name;

    /** 套餐编码 */
    private String code;

    /** 状态 */
    private String status;

    /** 排序号 */
    private Integer sort;
}
