package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantPageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenant.TenantUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenant.TenantResponse;
import io.github.fushuwei.scaskeleton.system.converter.TenantConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysTenant;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackage;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantPackageMapper;
import io.github.fushuwei.scaskeleton.system.service.SysTenantService;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 租户管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements SysTenantService {

    private static final String DEFAULT_TENANT_CODE = "default";

    private final SysTenantMapper tenantMapper;

    private final SysTenantPackageMapper packageMapper;

    private final TenantConverter tenantConverter;

    /**
     * 查询租户列表
     *
     * @return 租户列表
     */
    @Override
    public List<TenantResponse> listTenants() {
        // 查询全部租户，按 name 升序
        List<SysTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
            .eq(SysTenant::getIsDeleted, 0)
            .orderByAsc(SysTenant::getName));
        // 转换为响应对象列表
        return tenantConverter.toTenantResponseList(tenants);
    }

    /**
     * 分页查询租户列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<TenantResponse> pageTenants(TenantPageRequest request) {
        // 构造分页对象
        Page<TenantResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 查询分页数据
        return tenantMapper.selectTenantPage(page, request);
    }

    /**
     * 根据 ID 查询租户详情
     *
     * @param id 租户 ID
     * @return 租户详情
     */
    @Override
    public TenantResponse getTenantById(String id) {
        // 加载租户实体并转换为响应对象
        return tenantConverter.toTenantResponse(loadTenantEntity(id));
    }

    /**
     * 新增租户
     *
     * @param request 租户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTenant(TenantCreateRequest request) {
        // 仅超级管理员可创建租户，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可创建租户");
        }

        // 租户名称唯一
        long nameCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
            .eq(SysTenant::getName, request.getName()));
        if (nameCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "租户名称已存在");
        }

        // 租户编码唯一
        long codeCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
            .eq(SysTenant::getCode, request.getCode()));
        if (codeCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "租户编码已存在");
        }

        // 校验套餐存在且启用
        validatePackage(request.getPackageId());

        // 封装租户实体
        SysTenant tenant = new SysTenant();
        tenant.setName(request.getName());
        tenant.setCode(request.getCode());
        tenant.setPackageId(request.getPackageId());
        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setDomainName(request.getDomainName());
        tenant.setEffectiveTime(request.getEffectiveTime());
        tenant.setExpireTime(request.getExpireTime());
        tenant.setStatus(request.getStatus());
        tenant.setRemark(request.getRemark());

        // 保存租户
        tenantMapper.insert(tenant);
    }

    /**
     * 编辑租户
     *
     * @param request 租户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(TenantUpdateRequest request) {
        // 仅超级管理员可编辑租户，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可编辑租户");
        }

        // 加载租户实体
        SysTenant tenant = loadTenantEntity(request.getId());

        // 租户名称唯一（排除自身）
        long nameCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
            .eq(SysTenant::getName, request.getName())
            .ne(SysTenant::getId, request.getId()));
        if (nameCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "租户名称已存在");
        }

        // 租户编码唯一（排除自身）
        long codeCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
            .eq(SysTenant::getCode, request.getCode())
            .ne(SysTenant::getId, request.getId()));
        if (codeCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "租户编码已存在");
        }

        // 校验套餐存在且启用
        validatePackage(request.getPackageId());

        // 更新字段
        tenant.setName(request.getName());
        tenant.setCode(request.getCode());
        tenant.setPackageId(request.getPackageId());
        tenant.setContactName(request.getContactName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setDomainName(request.getDomainName());
        tenant.setEffectiveTime(request.getEffectiveTime());
        tenant.setExpireTime(request.getExpireTime());
        tenant.setStatus(request.getStatus());
        tenant.setRemark(request.getRemark());

        // 乐观锁：使用前端回传的 version 作为 WHERE 条件，若版本不匹配则影响行数为 0，说明数据已被其他用户修改
        tenant.setVersion(request.getVersion());
        int affectedRows = tenantMapper.updateById(tenant);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }
    }

    /**
     * 删除租户
     *
     * @param id 租户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(String id) {
        // 仅超级管理员可删除租户，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可删除租户");
        }

        // 加载租户实体并校验内置保护
        SysTenant tenant = loadTenantEntity(id);
        if (DEFAULT_TENANT_CODE.equals(tenant.getCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置租户不允许删除");
        }

        // 删除租户
        tenantMapper.deleteById(id);
    }

    /**
     * 批量删除租户
     *
     * @param ids 租户 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteTenants(List<String> ids) {
        // 仅超级管理员可批量删除租户，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可删除租户");
        }
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            deleteTenant(id);
        }
    }

    /**
     * 校验套餐存在且处于启用状态
     *
     * @param packageId 套餐 ID
     */
    private void validatePackage(String packageId) {
        SysTenantPackage pkg = packageMapper.selectById(packageId);
        if (pkg == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "所选套餐不存在");
        }
        if (!"enabled".equals(pkg.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "所选套餐未启用，无法关联租户");
        }
    }

    /**
     * 根据 ID 加载租户实体
     *
     * @param id 租户 ID
     * @return 租户实体
     */
    private SysTenant loadTenantEntity(String id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        return tenant;
    }
}
