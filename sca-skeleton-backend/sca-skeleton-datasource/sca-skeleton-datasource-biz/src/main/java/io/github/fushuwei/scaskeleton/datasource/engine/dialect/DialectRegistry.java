package io.github.fushuwei.scaskeleton.datasource.engine.dialect;

import io.github.fushuwei.scaskeleton.core.exception.BusinessException;
import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 方言注册中心。
 * <p>
 * 启动时收集所有 {@link Dialect} Spring Bean，按 {@link DbType} 分发。
 * 未注册的数据库类型调用时抛出业务异常。
 *
 * @author Fu Wei
 */
@Component
public class DialectRegistry {

    private final Map<DbType, Dialect> dialects = new EnumMap<>(DbType.class);

    /**
     * 构造时注入所有 Dialect Bean 并注册。
     *
     * @param dialectList Spring 自动收集的所有 Dialect 实现
     */
    public DialectRegistry(List<Dialect> dialectList) {
        for (Dialect dialect : dialectList) {
            dialects.put(dialect.dbType(), dialect);
        }
    }

    /**
     * 获取指定数据库类型的方言。
     *
     * @param dbType 数据库类型
     * @return 方言实例
     * @throws BusinessException 该类型未注册方言
     */
    public Dialect get(DbType dbType) {
        Dialect dialect = dialects.get(dbType);
        if (dialect == null) {
            throw new BusinessException("暂不支持的数据库类型: " + dbType + "，方言待实现");
        }
        return dialect;
    }
}
