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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 租户管理服务实现。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements SysTenantService {

    /** 默认租户编码，内置租户不允许删除 */
    private static final String DEFAULT_TENANT_CODE = "default";

    /** 租户 Mapper */
    private final SysTenantMapper tenantMapper;
    /** 套餐 Mapper（校验套餐存在性与状态） */
    private final SysTenantPackageMapper packageMapper;
    /** Entity ↔ Response 转换器（MapStruct 生成） */
    private final TenantConverter tenantConverter;

    @Override
    public IPage<TenantResponse> pageTenants(TenantPageRequest req) {
        Page<SysTenant> page = new Page<>(req.getPageNum(), req.getPageSize());

        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<SysTenant>()
                // 逻辑删除过滤（自定义 SQL 不自动追加 @TableLogic 条件，需显式指定）
                .eq(SysTenant::getIsDeleted, 0)
                // 关键词模糊匹配名称或编码
                .and(StringUtils.hasText(req.getKeyword()),
                        w -> w.like(SysTenant::getName, req.getKeyword())
                                .or().like(SysTenant::getCode, req.getKeyword()))
                // 状态筛选
                .eq(StringUtils.hasText(req.getStatus()), SysTenant::getStatus, req.getStatus())
                // 套餐筛选
                .eq(StringUtils.hasText(req.getPackageId()), SysTenant::getPackageId, req.getPackageId());

        // 安全排序：白名单校验通过后按指定字段排序，否则按创建时间降序
        String orderBy = req.safeOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeOrderDirection());
        if (orderBy != null) {
            switch (orderBy) {
                case "name" -> wrapper.orderBy(true, isAsc, SysTenant::getName);
                case "code" -> wrapper.orderBy(true, isAsc, SysTenant::getCode);
                case "status" -> wrapper.orderBy(true, isAsc, SysTenant::getStatus);
                case "effective_time" -> wrapper.orderBy(true, isAsc, SysTenant::getEffectiveTime);
                case "expire_time" -> wrapper.orderBy(true, isAsc, SysTenant::getExpireTime);
                case "create_time" -> wrapper.orderBy(true, isAsc, SysTenant::getCreateTime);
            }
        } else {
            wrapper.orderByDesc(SysTenant::getCreateTime);
        }

        IPage<SysTenant> entityPage = tenantMapper.selectTenantPage(page, wrapper);
        return entityPage.convert(tenantConverter::toTenantResponse);
    }

    @Override
    public List<TenantResponse> listTenants() {
        List<SysTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getIsDeleted, 0)
                .orderByAsc(SysTenant::getName));
        return tenants.stream().map(tenantConverter::toTenantResponse).toList();
    }

    @Override
    public TenantResponse getTenantById(String id) {
        return tenantConverter.toTenantResponse(loadTenantEntity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTenant(TenantCreateRequest req) {
        // 租户名称唯一
        long nameCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getName, req.getName()));
        if (nameCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "租户名称已存在");
        }
        // 租户编码唯一
        long codeCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getCode, req.getCode()));
        if (codeCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "租户编码已存在");
        }
        // 校验套餐存在且启用
        validatePackage(req.getPackageId());

        // 组装租户实体
        SysTenant tenant = new SysTenant();
        tenant.setName(req.getName());
        tenant.setCode(req.getCode());
        tenant.setPackageId(req.getPackageId());
        tenant.setContactName(req.getContactName());
        tenant.setContactPhone(req.getContactPhone());
        tenant.setContactEmail(req.getContactEmail());
        tenant.setDomainName(req.getDomainName());
        tenant.setEffectiveTime(req.getEffectiveTime());
        tenant.setExpireTime(req.getExpireTime());
        tenant.setStatus(req.getStatus());
        tenant.setRemark(req.getRemark());
        tenantMapper.insert(tenant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTenant(TenantUpdateRequest req) {
        // 校验租户存在并加载当前快照
        SysTenant existing = loadTenantEntity(req.getId());
        // 租户名称唯一（排除自身）
        long nameCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getName, req.getName())
                .ne(SysTenant::getId, req.getId()));
        if (nameCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "租户名称已存在");
        }
        // 租户编码唯一（排除自身）
        long codeCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getCode, req.getCode())
                .ne(SysTenant::getId, req.getId()));
        if (codeCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "租户编码已存在");
        }
        // 校验套餐存在且启用
        validatePackage(req.getPackageId());

        existing.setName(req.getName());
        existing.setCode(req.getCode());
        existing.setPackageId(req.getPackageId());
        existing.setContactName(req.getContactName());
        existing.setContactPhone(req.getContactPhone());
        existing.setContactEmail(req.getContactEmail());
        existing.setDomainName(req.getDomainName());
        existing.setEffectiveTime(req.getEffectiveTime());
        existing.setExpireTime(req.getExpireTime());
        existing.setStatus(req.getStatus());
        existing.setRemark(req.getRemark());
        tenantMapper.updateById(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTenant(String id) {
        // 加载待删租户并校验内置保护
        SysTenant tenant = loadTenantEntity(id);
        if (DEFAULT_TENANT_CODE.equals(tenant.getCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置租户不允许删除");
        }
        tenantMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteTenants(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            deleteTenant(id);
        }
    }

    /**
     * 校验套餐存在且处于启用状态。
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
     * 按主键加载租户实体（供内部业务逻辑使用，不对外暴露 Entity）。
     */
    private SysTenant loadTenantEntity(String id) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "租户不存在");
        }
        return tenant;
    }
}
