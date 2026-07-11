package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageUpdateRequest;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;
import io.github.fushuwei.scaskeleton.system.converter.TenantPackageConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysPermission;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackage;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackagePermission;
import io.github.fushuwei.scaskeleton.system.mapper.SysPermissionMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantPackageMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantPackagePermissionMapper;
import io.github.fushuwei.scaskeleton.system.service.SysTenantPackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 租户套餐管理服务实现。
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantPackageServiceImpl implements SysTenantPackageService {

    /** 默认套餐编码，内置套餐不允许删除 */
    private static final String DEFAULT_PACKAGE_CODE = "default";

    /** 权限 Mapper */
    private final SysPermissionMapper permissionMapper;
    /** 套餐主表 Mapper */
    private final SysTenantPackageMapper packageMapper;
    /** 套餐-权限关联 Mapper */
    private final SysTenantPackagePermissionMapper packagePermissionMapper;
    /** Entity ↔ Response 转换器（MapStruct 生成） */
    private final TenantPackageConverter packageConverter;

    @Override
    public IPage<TenantPackageResponse> pagePackages(TenantPackagePageRequest req) {
        // 构造分页对象
        Page<SysTenantPackage> page = new Page<>(req.getPageNum(), req.getPageSize());

        LambdaQueryWrapper<SysTenantPackage> wrapper = new LambdaQueryWrapper<SysTenantPackage>()
                // 逻辑删除过滤（自定义 SQL 不自动追加 @TableLogic 条件，需显式指定）
                .eq(SysTenantPackage::getIsDeleted, 0)
                // 关键词模糊匹配名称或编码
                .and(StringUtils.hasText(req.getKeyword()),
                        w -> w.like(SysTenantPackage::getName, req.getKeyword())
                                .or().like(SysTenantPackage::getCode, req.getKeyword()))
                // 状态筛选
                .eq(StringUtils.hasText(req.getStatus()), SysTenantPackage::getStatus, req.getStatus());

        // 安全排序：白名单校验通过后按指定字段排序，否则按 sort 升序
        String orderBy = req.safeOrderBy();
        boolean isAsc = "ASC".equalsIgnoreCase(req.safeOrderDirection());
        if (orderBy != null) {
            switch (orderBy) {
                case "name" -> wrapper.orderBy(true, isAsc, SysTenantPackage::getName);
                case "code" -> wrapper.orderBy(true, isAsc, SysTenantPackage::getCode);
                case "status" -> wrapper.orderBy(true, isAsc, SysTenantPackage::getStatus);
                case "sort" -> wrapper.orderBy(true, isAsc, SysTenantPackage::getSort);
                case "create_time" -> wrapper.orderBy(true, isAsc, SysTenantPackage::getCreateTime);
                case "permission_count" -> wrapper.orderBy(true, isAsc, SysTenantPackage::getPermissionCount);
            }
        } else {
            wrapper.orderByAsc(SysTenantPackage::getSort);
        }

        // 查询实体分页并转换为响应对象分页
        IPage<SysTenantPackage> entityPage = packageMapper.selectPackagePage(page, wrapper);
        return entityPage.convert(packageConverter::toTenantPackageResponse);
    }

    @Override
    public List<TenantPackageResponse> listPackages() {
        // 查询全部套餐，按 sort 升序
        List<SysTenantPackage> packages = packageMapper.selectList(new LambdaQueryWrapper<SysTenantPackage>()
                .orderByAsc(SysTenantPackage::getSort));
        return packages.stream().map(packageConverter::toTenantPackageResponse).toList();
    }

    @Override
    public TenantPackageResponse getPackageById(String id) {
        // 按主键查询套餐并转换为响应对象
        return packageConverter.toTenantPackageResponse(loadPackageEntity(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPackage(TenantPackageCreateRequest req) {
        // 套餐名称唯一
        long nameCount = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getName, req.getName()));
        if (nameCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "套餐名称已存在");
        }
        // 套餐编码唯一
        long codeCount = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getCode, req.getCode()));
        if (codeCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "套餐编码已存在");
        }
        // 组装套餐实体
        SysTenantPackage pkg = new SysTenantPackage();
        pkg.setName(req.getName());
        pkg.setCode(req.getCode());
        pkg.setStatus(req.getStatus());
        pkg.setUserLimit(req.getUserLimit() != null ? req.getUserLimit() : -1);
        pkg.setApiLimit(req.getApiLimit() != null ? req.getApiLimit() : -1);
        pkg.setStorageLimit(req.getStorageLimit() != null ? req.getStorageLimit() : -1);
        pkg.setExpireDays(req.getExpireDays() != null ? req.getExpireDays() : -1);
        pkg.setSort(req.getSort() != null ? req.getSort() : 100);
        pkg.setRemark(req.getRemark());
        // 持久化套餐主表
        packageMapper.insert(pkg);
        // 同事务内建立套餐-权限关联
        savePackagePermissions(pkg.getId(), req.getPermissionIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePackage(TenantPackageUpdateRequest req) {
        // 校验套餐存在并加载当前快照
        SysTenantPackage existing = loadPackageEntity(req.getId());
        // 套餐名称唯一（排除自身）
        long nameCount = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getName, req.getName())
                .ne(SysTenantPackage::getId, req.getId()));
        if (nameCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "套餐名称已存在");
        }
        // 套餐编码唯一（排除自身）
        long codeCount = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getCode, req.getCode())
                .ne(SysTenantPackage::getId, req.getId()));
        if (codeCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "套餐编码已存在");
        }
        existing.setName(req.getName());
        existing.setCode(req.getCode());
        existing.setStatus(req.getStatus());
        existing.setUserLimit(req.getUserLimit() != null ? req.getUserLimit() : -1);
        existing.setApiLimit(req.getApiLimit() != null ? req.getApiLimit() : -1);
        existing.setStorageLimit(req.getStorageLimit() != null ? req.getStorageLimit() : -1);
        existing.setExpireDays(req.getExpireDays() != null ? req.getExpireDays() : -1);
        existing.setSort(req.getSort() != null ? req.getSort() : existing.getSort());
        existing.setRemark(req.getRemark());
        packageMapper.updateById(existing);
        // 清除旧关联，重新建立
        packagePermissionMapper.delete(new LambdaQueryWrapper<SysTenantPackagePermission>()
                .eq(SysTenantPackagePermission::getPackageId, req.getId()));
        savePackagePermissions(req.getId(), req.getPermissionIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(String id) {
        // 加载待删套餐并校验内置保护
        SysTenantPackage pkg = loadPackageEntity(id);
        if (DEFAULT_PACKAGE_CODE.equals(pkg.getCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置套餐不允许删除");
        }
        // 逻辑删除套餐主表
        packageMapper.deleteById(id);
        // 同事务内清理套餐-权限关联
        packagePermissionMapper.delete(new LambdaQueryWrapper<SysTenantPackagePermission>()
                .eq(SysTenantPackagePermission::getPackageId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePackages(List<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        for (String id : ids) {
            deletePackage(id);
        }
    }

    @Override
    public List<String> getPackagePermissionIds(String packageId) {
        SysTenantPackage pkg = loadPackageEntity(packageId);
        // 内置默认套餐拥有全部权限，返回 menu 和 button 类别的权限 ID
        if (DEFAULT_PACKAGE_CODE.equals(pkg.getCode())) {
            return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                            .in(SysPermission::getType, "menu", "button")).stream()
                    .map(SysPermission::getId)
                    .collect(Collectors.toList());
        }
        // 查询套餐已分配的权限 ID 列表
        List<SysTenantPackagePermission> list = packagePermissionMapper.selectList(
                new LambdaQueryWrapper<SysTenantPackagePermission>()
                        .eq(SysTenantPackagePermission::getPackageId, packageId));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(SysTenantPackagePermission::getPermissionId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(String packageId, List<String> permissionIds) {
        // 校验套餐存在
        loadPackageEntity(packageId);
        // 先清空该套餐下原有权限关联（全量替换策略）
        packagePermissionMapper.delete(new LambdaQueryWrapper<SysTenantPackagePermission>()
                .eq(SysTenantPackagePermission::getPackageId, packageId));
        // 非空则逐条插入新的套餐-权限关联
        savePackagePermissions(packageId, permissionIds);
    }

    /**
     * 批量插入套餐-权限关联记录。
     *
     * @param packageId     套餐 ID
     * @param permissionIds 权限 ID 列表，为空则不操作
     */
    private void savePackagePermissions(String packageId, List<String> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        permissionIds.forEach(permId -> {
            SysTenantPackagePermission tpp = new SysTenantPackagePermission();
            tpp.setPackageId(packageId);
            tpp.setPermissionId(permId);
            packagePermissionMapper.insert(tpp);
        });
    }

    /**
     * 按主键加载套餐实体（供内部业务逻辑使用，不对外暴露 Entity）。
     *
     * @param id 套餐 ID
     * @return 套餐实体
     * @throws BusinessException 套餐不存在时抛出 NOT_FOUND
     */
    private SysTenantPackage loadPackageEntity(String id) {
        SysTenantPackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "套餐不存在");
        }
        return pkg;
    }
}
