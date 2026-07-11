package io.github.fushuwei.scaskeleton.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenant.TenantResponse;

import java.util.List;

/**
 * 租户管理服务接口。
 *
 * @author Fu Wei
 */
public interface SysTenantService {

    /** 分页查询租户 */
    IPage<TenantResponse> pageTenants(TenantPageRequest request);

    List<TenantResponse> listTenants();

    TenantResponse getTenantById(String id);

    void createTenant(TenantCreateRequest request);

    void updateTenant(TenantUpdateRequest request);

    void deleteTenant(String id);

    /** 批量删除租户 */
    void batchDeleteTenants(List<String> ids);
}
