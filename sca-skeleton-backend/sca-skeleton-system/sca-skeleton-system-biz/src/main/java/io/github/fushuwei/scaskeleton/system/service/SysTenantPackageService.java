package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePermissionAssignRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;

import java.util.List;

/**
 * 租户套餐管理 Service
 *
 * @author Fu Wei
 */
public interface SysTenantPackageService {

    /**
     * 查询套餐列表
     *
     * @return 套餐列表
     */
    List<TenantPackageResponse> listPackages();

    /**
     * 查询套餐选项列表
     *
     * @return 套餐选项列表
     */
    List<TenantPackageOptionResponse> listPackageOptions();

    /**
     * 分页查询套餐列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<TenantPackageResponse> pagePackages(TenantPackagePageRequest request);

    /**
     * 根据 ID 查询套餐详情
     *
     * @param id 套餐 ID
     * @return 套餐详情
     */
    TenantPackageResponse getPackageById(String id);

    /**
     * 新增套餐
     *
     * @param request 套餐信息
     */
    void createPackage(TenantPackageCreateRequest request);

    /**
     * 编辑套餐
     *
     * @param request 套餐信息
     */
    void updatePackage(TenantPackageUpdateRequest request);

    /**
     * 删除套餐
     *
     * @param id 套餐 ID
     */
    void deletePackage(String id);

    /**
     * 批量删除套餐
     *
     * @param ids 套餐 ID 列表
     */
    void batchDeletePackages(List<String> ids);

    /**
     * 查询套餐已分配的权限 ID 列表
     *
     * @param packageId 套餐 ID
     * @return 权限 ID 列表
     */
    List<String> getPackagePermissionIds(String packageId);

    /**
     * 为套餐分配权限
     *
     * @param request 权限分配信息
     */
    void assignPermissions(TenantPackagePermissionAssignRequest request);
}
