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

    /** SysTenantPackage → TenantPackageResponse */
    TenantPackageResponse toTenantPackageResponse(SysTenantPackage pkg);
}
