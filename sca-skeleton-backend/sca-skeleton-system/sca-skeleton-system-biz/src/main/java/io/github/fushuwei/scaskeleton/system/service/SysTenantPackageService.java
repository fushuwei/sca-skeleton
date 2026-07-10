package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;

import java.util.List;

/**
 * 租户套餐管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysTenantPackageService {

    /** 分页查询套餐 */
    IPage<TenantPackageResponse> pagePackages(TenantPackagePageRequest request);

    List<TenantPackageResponse> listPackages();

    TenantPackageResponse getPackageById(String id);

    void createPackage(TenantPackageCreateRequest request);

    void updatePackage(TenantPackageUpdateRequest request);

    void deletePackage(String id);

    /** 批量删除套餐 */
    void batchDeletePackages(List<String> ids);

    /** 查询套餐已分配的权限 ID 列表 */
    List<String> getPackagePermissionIds(String packageId);

    /** 为套餐分配权限（全量替换） */
    void assignPermissions(String packageId, List<String> permissionIds);
}
