package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackage;
import org.mapstruct.Mapper;

/**
 * 租户套餐对象转换器（MapStruct）
 *
 * @author Fu Wei
 */
@Mapper(componentModel = "spring")
public interface TenantPackageConverter {

    /**
     * 租户套餐实体 → 租户套餐响应
     *
     * @param pkg 租户套餐实体
     * @return 租户套餐响应对象
     */
    TenantPackageResponse toTenantPackageResponse(SysTenantPackage pkg);
}
