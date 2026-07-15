package io.github.fushuwei.scaskeleton.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.result.ResultCode;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePageRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackageCreateRequest;
import io.github.fushuwei.scaskeleton.system.api.request.tenantpackage.TenantPackagePermissionAssignRequest;
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

import java.util.Collections;
import java.util.List;

/**
 * 租户套餐管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantPackageServiceImpl implements SysTenantPackageService {

    private static final String DEFAULT_PACKAGE_CODE = "default";

    private final SysPermissionMapper permissionMapper;

    private final SysTenantPackageMapper packageMapper;

    private final SysTenantPackagePermissionMapper packagePermissionMapper;

    private final TenantPackageConverter packageConverter;

    /**
     * 查询套餐列表
     *
     * @return 套餐列表
     */
    @Override
    public List<TenantPackageResponse> listPackages() {
        // 查询全部套餐，按 sort 升序
        List<SysTenantPackage> packages = packageMapper.selectList(new LambdaQueryWrapper<SysTenantPackage>()
            .orderByAsc(SysTenantPackage::getSort));
        // 转换为响应对象列表
        return packages.stream().map(packageConverter::toTenantPackageResponse).toList();
    }

    /**
     * 分页查询套餐列表
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Override
    public IPage<TenantPackageResponse> pagePackages(TenantPackagePageRequest request) {
        // 构造分页对象
        Page<SysTenantPackage> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 查询分页数据，并将结果转换为响应对象
        IPage<SysTenantPackage> entityPage = packageMapper.selectPackagePage(page, request);
        return entityPage.convert(packageConverter::toTenantPackageResponse);
    }

    /**
     * 根据 ID 查询套餐详情
     *
     * @param id 套餐 ID
     * @return 套餐详情
     */
    @Override
    public TenantPackageResponse getPackageById(String id) {
        // 加载套餐实体并转换为响应对象
        return packageConverter.toTenantPackageResponse(loadPackageEntity(id));
    }

    /**
     * 新增套餐
     *
     * @param request 套餐信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPackage(TenantPackageCreateRequest request) {
        // 套餐名称唯一
        long nameCount = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
            .eq(SysTenantPackage::getName, request.getName()));
        if (nameCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "套餐名称已存在");
        }

        // 套餐编码唯一
        long codeCount = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
            .eq(SysTenantPackage::getCode, request.getCode()));
        if (codeCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "套餐编码已存在");
        }

        // 封装套餐实体
        SysTenantPackage pkg = new SysTenantPackage();
        pkg.setName(request.getName());
        pkg.setCode(request.getCode());
        pkg.setStatus(request.getStatus());
        pkg.setUserLimit(request.getUserLimit() != null ? request.getUserLimit() : -1);
        pkg.setApiLimit(request.getApiLimit() != null ? request.getApiLimit() : -1);
        pkg.setStorageLimit(request.getStorageLimit() != null ? request.getStorageLimit() : -1);
        pkg.setExpireDays(request.getExpireDays() != null ? request.getExpireDays() : -1);
        pkg.setSort(request.getSort() != null ? request.getSort() : 100);
        pkg.setRemark(request.getRemark());

        // 保存套餐
        packageMapper.insert(pkg);

        // 保存关联关系
        savePackagePermissions(pkg.getId(), request.getPermissionIds());
    }

    /**
     * 编辑套餐
     *
     * @param request 套餐信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePackage(TenantPackageUpdateRequest request) {
        // 加载套餐实体
        SysTenantPackage pkg = loadPackageEntity(request.getId());

        // 套餐名称唯一（排除自身）
        long nameCount = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
            .eq(SysTenantPackage::getName, request.getName())
            .ne(SysTenantPackage::getId, request.getId()));
        if (nameCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "套餐名称已存在");
        }

        // 套餐编码唯一（排除自身）
        long codeCount = packageMapper.selectCount(new LambdaQueryWrapper<SysTenantPackage>()
            .eq(SysTenantPackage::getCode, request.getCode())
            .ne(SysTenantPackage::getId, request.getId()));
        if (codeCount > 0) {
            throw new BusinessException(ResultCode.ALREADY_EXISTS, "套餐编码已存在");
        }

        // 更新字段
        pkg.setName(request.getName());
        pkg.setCode(request.getCode());
        pkg.setStatus(request.getStatus());
        pkg.setUserLimit(request.getUserLimit() != null ? request.getUserLimit() : -1);
        pkg.setApiLimit(request.getApiLimit() != null ? request.getApiLimit() : -1);
        pkg.setStorageLimit(request.getStorageLimit() != null ? request.getStorageLimit() : -1);
        pkg.setExpireDays(request.getExpireDays() != null ? request.getExpireDays() : -1);
        pkg.setSort(request.getSort() != null ? request.getSort() : pkg.getSort());
        pkg.setRemark(request.getRemark());

        // 更新套餐
        packageMapper.updateById(pkg);

        // 删除旧的关联关系，并保存新的关联关系
        deletePackagePermissions(request.getId());
        savePackagePermissions(request.getId(), request.getPermissionIds());
    }

    /**
     * 删除套餐
     *
     * @param id 套餐 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePackage(String id) {
        // 加载套餐实体并校验内置保护
        SysTenantPackage pkg = loadPackageEntity(id);
        if (DEFAULT_PACKAGE_CODE.equals(pkg.getCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "系统内置套餐不允许删除");
        }

        // 删除套餐
        packageMapper.deleteById(id);

        // 删除关联关系
        deletePackagePermissions(id);
    }

    /**
     * 批量删除套餐
     *
     * @param ids 套餐 ID 列表
     */
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

    /**
     * 查询套餐已分配的权限 ID 列表
     *
     * @param packageId 套餐 ID
     * @return 权限 ID 列表
     */
    @Override
    public List<String> getPackagePermissionIds(String packageId) {
        // 校验套餐存在
        SysTenantPackage pkg = loadPackageEntity(packageId);

        // 内置默认套餐拥有全部权限，返回 menu 和 button 类别的权限 ID
        if (DEFAULT_PACKAGE_CODE.equals(pkg.getCode())) {
            return permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                    .in(SysPermission::getType, "menu", "button")).stream()
                .map(SysPermission::getId)
                .toList();
        }

        // 查询套餐已分配的权限 ID 列表
        List<SysTenantPackagePermission> list = packagePermissionMapper.selectList(
            new LambdaQueryWrapper<SysTenantPackagePermission>()
                .eq(SysTenantPackagePermission::getPackageId, packageId));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(SysTenantPackagePermission::getPermissionId).toList();
    }

    /**
     * 为套餐分配权限
     *
     * @param request 权限分配信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(TenantPackagePermissionAssignRequest request) {
        // 加载套餐实体
        loadPackageEntity(request.getId());

        // 先清空该套餐下原有权限关联（全量替换策略）
        deletePackagePermissions(request.getId());

        // 保存新的套餐-权限关联
        savePackagePermissions(request.getId(), request.getPermissionIds());
    }

    /**
     * 保存套餐与权限的关联关系
     *
     * @param packageId     套餐 ID
     * @param permissionIds 权限 ID 列表
     */
    private void savePackagePermissions(String packageId, List<String> permissionIds) {
        // 保存套餐与权限关联关系
        if (!CollectionUtils.isEmpty(permissionIds)) {
            permissionIds.forEach(permId -> {
                SysTenantPackagePermission tpp = new SysTenantPackagePermission();
                tpp.setPackageId(packageId);
                tpp.setPermissionId(permId);
                packagePermissionMapper.insert(tpp);
            });
        }
    }

    /**
     * 删除套餐与权限的关联关系
     *
     * @param packageId 套餐 ID
     */
    private void deletePackagePermissions(String packageId) {
        packagePermissionMapper.delete(new LambdaQueryWrapper<SysTenantPackagePermission>()
            .eq(SysTenantPackagePermission::getPackageId, packageId));
    }

    /**
     * 根据 ID 加载套餐实体
     *
     * @param id 套餐 ID
     * @return 套餐实体
     */
    private SysTenantPackage loadPackageEntity(String id) {
        SysTenantPackage pkg = packageMapper.selectById(id);
        if (pkg == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "套餐不存在");
        }
        return pkg;
    }
}
