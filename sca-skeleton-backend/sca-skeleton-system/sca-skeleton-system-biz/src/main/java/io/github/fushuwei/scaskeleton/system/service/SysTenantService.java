package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenant.TenantResponse;

import java.util.List;

/**
 * 租户管理 Service
 *
 * @author Fu Wei
 */
public interface SysTenantService {

    /**
     * 查询租户列表
     *
     * @return 租户列表
     */
    List<TenantResponse> listTenants();

    /**
     * 分页查询租户列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<TenantResponse> pageTenants(TenantPageRequest request);

    /**
     * 根据 ID 查询租户详情
     *
     * @param id 租户 ID
     * @return 租户详情
     */
    TenantResponse getTenantById(String id);

    /**
     * 新增租户
     *
     * @param request 租户信息
     */
    void createTenant(TenantCreateRequest request);

    /**
     * 编辑租户
     *
     * @param request 租户信息
     */
    void updateTenant(TenantUpdateRequest request);

    /**
     * 删除租户
     *
     * @param id 租户 ID
     */
    void deleteTenant(String id);

    /**
     * 批量删除租户
     *
     * @param ids 租户 ID 列表
     */
    void batchDeleteTenants(List<String> ids);
}
