package io.github.fushuwei.scaskeleton.system.converter;

import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackage;
import org.mapstruct.Mapper;

import java.util.List;

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

    /**
     * 租户套餐实体列表 → 租户套餐响应列表
     *
     * @param packages 租户套餐实体列表
     * @return 租户套餐响应列表
     */
    List<TenantPackageResponse> toTenantPackageResponseList(List<SysTenantPackage> packages);

    /**
     * 租户套餐实体 → 租户套餐选项响应
     *
     * @param pkg 租户套餐实体
     * @return 租户套餐选项响应对象
     */
    TenantPackageOptionResponse toTenantPackageOptionResponse(SysTenantPackage pkg);

    /**
     * 租户套餐实体列表 → 租户套餐选项响应列表
     *
     * @param packages 租户套餐实体列表
     * @return 租户套餐选项响应列表
     */
    List<TenantPackageOptionResponse> toTenantPackageOptionResponseList(List<SysTenantPackage> packages);
}
