package io.github.fushuwei.scaskeleton.datasource.engine.dialect;

import io.github.fushuwei.scaskeleton.datasource.api.enums.DbType;
import org.springframework.stereotype.Component;

/**
 * MySQL 方言。
 *
 * @author Fu Wei
 */
@Component
public class MysqlDialect extends AbstractJdbcDialect {

    @Override
    public DbType dbType() {
        return DbType.MYSQL;
    }

    @Override
    protected String urlPrefix() {
        return "jdbc:mysql://";
    }

    @Override
    public String pingSql() {
        return "SELECT 1";
    }
}
