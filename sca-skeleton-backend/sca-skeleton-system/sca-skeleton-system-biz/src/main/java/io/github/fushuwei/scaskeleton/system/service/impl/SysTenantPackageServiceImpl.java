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
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageOptionResponse;
import io.github.fushuwei.scaskeleton.system.api.response.tenantpackage.TenantPackageResponse;
import io.github.fushuwei.scaskeleton.system.converter.TenantPackageConverter;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackage;
import io.github.fushuwei.scaskeleton.system.entity.SysTenantPackagePermission;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantPackageMapper;
import io.github.fushuwei.scaskeleton.system.mapper.SysTenantPackagePermissionMapper;
import io.github.fushuwei.scaskeleton.mybatis.reference.ReferenceChecker;
import io.github.fushuwei.scaskeleton.system.service.SysTenantPackageService;
import io.github.fushuwei.scaskeleton.security.context.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户套餐管理 Service 实现类
 *
 * @author Fu Wei
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantPackageServiceImpl implements SysTenantPackageService {

    private final SysTenantPackageMapper packageMapper;

    private final SysTenantPackagePermissionMapper packagePermissionMapper;

    private final TenantPackageConverter packageConverter;

    private final ReferenceChecker referenceChecker;

    /**
     * 查询套餐列表
     *
     * @return 套餐列表
     */
    @Override
    public List<TenantPackageResponse> listPackages() {
        // 查询全部套餐
        List<SysTenantPackage> packages = packageMapper.selectList(new LambdaQueryWrapper<SysTenantPackage>()
            .orderByAsc(SysTenantPackage::getSort));
        // 转换为响应对象列表
        return packageConverter.toTenantPackageResponseList(packages);
    }

    /**
     * 查询套餐选项列表
     *
     * @return 套餐选项列表
     */
    @Override
    public List<TenantPackageOptionResponse> listPackageOptions() {
        // 查询启用的套餐
        List<SysTenantPackage> packages = packageMapper.selectList(new LambdaQueryWrapper<SysTenantPackage>()
            .eq(SysTenantPackage::getStatus, "enabled")
            .orderByAsc(SysTenantPackage::getSort));
        // 转换为响应对象列表
        return packageConverter.toTenantPackageOptionResponseList(packages);
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
        Page<TenantPackageResponse> page = new Page<>(request.getPageNum(), request.getPageSize());
        // 查询分页数据
        return packageMapper.selectPackagePage(page, request);
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
        // 仅超级管理员可创建套餐，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可创建套餐");
        }

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
        // 仅超级管理员可编辑套餐，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可编辑套餐");
        }

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

        // 乐观锁：使用前端回传的 version 作为 WHERE 条件，若版本不匹配则影响行数为 0，说明数据已被其他用户修改
        pkg.setVersion(request.getVersion());
        int affectedRows = packageMapper.updateById(pkg);
        if (affectedRows == 0) {
            throw new BusinessException(ResultCode.VERSION_CONFLICT);
        }

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
        // 仅超级管理员可删除套餐，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可删除套餐");
        }

        // 加载套餐实体
        loadPackageEntity(id);

        // 引用校验
        referenceChecker.check(SysTenantPackage.class, id);

        // 删除关联关系
        deletePackagePermissions(id);

        // 删除套餐
        packageMapper.deleteById(id);
    }

    /**
     * 批量删除套餐
     *
     * @param ids 套餐 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePackages(List<String> ids) {
        // 仅超级管理员可批量删除套餐，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可删除套餐");
        }
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // 批量加载套餐实体并校验存在
        loadPackageEntities(ids);

        // 引用校验
        referenceChecker.checkBatch(SysTenantPackage.class, ids);

        // 批量删除关联关系
        deletePackagePermissions(ids);

        // 批量删除套餐
        packageMapper.deleteBatchIds(ids);
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
        loadPackageEntity(packageId);

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
        // 仅超级管理员可为套餐分配权限，防止其他用户伪造数据
        if (!SecurityUtils.isSuperAdmin()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅超级管理员可为套餐分配权限");
        }

        // 加载套餐实体
        loadPackageEntity(request.getId());

        // 先清空该套餐下原有权限关联关系
        deletePackagePermissions(request.getId());

        // 保存新的套餐与权限关联关系
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
        deletePackagePermissions(Collections.singletonList(packageId));
    }

    /**
     * 批量删除套餐与权限的关联关系
     *
     * @param packageIds 套餐 ID 列表
     */
    private void deletePackagePermissions(List<String> packageIds) {
        if (CollectionUtils.isEmpty(packageIds)) {
            return;
        }
        packagePermissionMapper.delete(new LambdaQueryWrapper<SysTenantPackagePermission>()
            .in(SysTenantPackagePermission::getPackageId, packageIds));
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

    /**
     * 根据 ID 列表批量加载套餐实体并校验存在性
     *
     * @param ids 套餐 ID 列表
     * @return 套餐实体列表
     */
    private List<SysTenantPackage> loadPackageEntities(List<String> ids) {
        List<String> distinctIds = ids.stream().distinct().toList();
        List<SysTenantPackage> entities = packageMapper.selectBatchIds(distinctIds);
        if (entities.size() != distinctIds.size()) {
            Set<String> foundIds = entities.stream().map(SysTenantPackage::getId).collect(Collectors.toSet());
            List<String> missing = distinctIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BusinessException(ResultCode.NOT_FOUND, "套餐不存在，ID: " + String.join(", ", missing));
        }
        return entities;
    }
}
