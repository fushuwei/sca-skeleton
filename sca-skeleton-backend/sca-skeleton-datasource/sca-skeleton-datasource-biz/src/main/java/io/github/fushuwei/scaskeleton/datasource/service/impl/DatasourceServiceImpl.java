package io.github.fushuwei.scaskeleton.datasource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.core.uuid.UuidUtils;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceCreateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourcePageRequest;
import io.github.fushuwei.scaskeleton.datasource.api.request.datasource.DatasourceUpdateRequest;
import io.github.fushuwei.scaskeleton.datasource.api.response.datasource.DatasourceResponse;
import io.github.fushuwei.scaskeleton.datasource.converter.DatasourceConverter;
import io.github.fushuwei.scaskeleton.datasource.entity.Datasource;
import io.github.fushuwei.scaskeleton.datasource.entity.Driver;
import io.github.fushuwei.scaskeleton.datasource.mapper.DatasourceMapper;
import io.github.fushuwei.scaskeleton.datasource.mapper.DriverMapper;
import io.github.fushuwei.scaskeleton.datasource.service.DatasourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 数据源管理 Service 实现（M0 骨架版）
 * <p>
 * M0 阶段仅实现 CRUD 骨架，连接测试、元数据浏览等在 M2 实现。
 *
 * @author Fu Wei
 */
@Service
@RequiredArgsConstructor
public class DatasourceServiceImpl implements DatasourceService {

    private final DatasourceMapper datasourceMapper;
    private final DriverMapper driverMapper;
    private final DatasourceConverter datasourceConverter;

    @Override
    public IPage<DatasourceResponse> pageDatasources(DatasourcePageRequest request) {
        Page<Datasource> page = new Page<>(request.getPageNum(), request.getPageSize());
        LambdaQueryWrapper<Datasource> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getDbType())) {
            wrapper.eq(Datasource::getDbType, request.getDbType());
        }
        if (request.getEnabled() != null) {
            wrapper.eq(Datasource::getEnabled, request.getEnabled());
        }
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(Datasource::getName, request.getKeyword())
                .or().like(Datasource::getHost, request.getKeyword()));
        }
        wrapper.orderByDesc(Datasource::getCreateTime);

        IPage<Datasource> dsPage = datasourceMapper.selectPage(page, wrapper);
        return dsPage.convert(datasourceConverter::toDatasourceResponse);
    }

    @Override
    public DatasourceResponse getDatasourceById(String id) {
        Datasource datasource = datasourceMapper.selectById(id);
        if (datasource == null) {
            throw new BusinessException("数据源不存在");
        }
        return datasourceConverter.toDatasourceResponse(datasource);
    }

    @Override
    public void createDatasource(DatasourceCreateRequest request) {
        // 校验数据源与驱动一致性
        if (StringUtils.hasText(request.getDriverId())) {
            Driver driver = driverMapper.selectById(request.getDriverId());
            if (driver == null) {
                throw new BusinessException("关联驱动不存在");
            }
            if (!driver.getDbType().equals(request.getDbType().name())) {
                throw new BusinessException("驱动数据库类型与数据源类型不一致");
            }
            if (!"enabled".equals(driver.getStatus())) {
                throw new BusinessException("关联驱动已禁用");
            }
        }

        Datasource datasource = datasourceConverter.toDatasource(request);
        datasource.setId(UuidUtils.nextSimpleStr());
        datasource.setEnabled(1);
        datasource.setConnectionState("offline");
        // TODO M2: 凭据 AES-GCM 加密
        datasource.setPasswordCipher(request.getPassword());
        datasourceMapper.insert(datasource);
    }

    @Override
    public void updateDatasource(DatasourceUpdateRequest request) {
        Datasource datasource = datasourceMapper.selectById(request.getId());
        if (datasource == null) {
            throw new BusinessException("数据源不存在");
        }
        if (StringUtils.hasText(request.getName())) {
            datasource.setName(request.getName());
        }
        if (request.getDbType() != null) {
            datasource.setDbType(request.getDbType().name());
        }
        if (StringUtils.hasText(request.getDriverId())) {
            datasource.setDriverId(request.getDriverId());
        }
        if (StringUtils.hasText(request.getHost())) {
            datasource.setHost(request.getHost());
        }
        if (request.getPort() != null) {
            datasource.setPort(request.getPort());
        }
        if (StringUtils.hasText(request.getDatabaseName())) {
            datasource.setDatabaseName(request.getDatabaseName());
        }
        if (StringUtils.hasText(request.getUsername())) {
            datasource.setUsername(request.getUsername());
        }
        if (StringUtils.hasText(request.getPassword())) {
            // TODO M2: 凭据 AES-GCM 加密
            datasource.setPasswordCipher(request.getPassword());
        }
        if (StringUtils.hasText(request.getConnectionParams())) {
            datasource.setConnectionParams(request.getConnectionParams());
        }
        if (StringUtils.hasText(request.getPoolConfig())) {
            datasource.setPoolConfig(request.getPoolConfig());
        }
        datasourceMapper.updateById(datasource);
    }

    @Override
    public void deleteDatasource(String id) {
        Datasource datasource = datasourceMapper.selectById(id);
        if (datasource == null) {
            throw new BusinessException("数据源不存在");
        }
        datasourceMapper.deleteById(id);
    }

    @Override
    public DatasourceResponse testConnection(String id) {
        // TODO M2: 使用 Driver 实例直连测试
        throw new BusinessException("连接测试功能在 M2 阶段实现");
    }

    @Override
    public void changeEnabled(String id, Integer enabled) {
        Datasource datasource = datasourceMapper.selectById(id);
        if (datasource == null) {
            throw new BusinessException("数据源不存在");
        }
        datasource.setEnabled(enabled);
        datasourceMapper.updateById(datasource);
    }

    @Override
    public List<DbType> listDbTypes() {
        return Arrays.asList(DbType.values());
    }

    @Override
    public List<String> listDatabases(String datasourceId) {
        // TODO M2: 元数据浏览
        throw new BusinessException("元数据浏览功能在 M2 阶段实现");
    }

    @Override
    public List<String> listTables(String datasourceId, String database) {
        // TODO M2: 元数据浏览
        throw new BusinessException("元数据浏览功能在 M2 阶段实现");
    }

    @Override
    public List<String> listColumns(String datasourceId, String table) {
        // TODO M2: 元数据浏览
        throw new BusinessException("元数据浏览功能在 M2 阶段实现");
    }
}
