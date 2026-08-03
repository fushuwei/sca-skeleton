package io.github.fushuwei.scaskeleton.datasource.api.enums;

import lombok.Getter;

/**
 * 数据库类型枚举。
 * <p>
 * 对外称「10 类库」，OceanBase 不区分 MySQL/Oracle 兼容模式，统一视为一种数据库类型，共 10 个枚举值。
 * 权限种子与前端下拉同步 10 个枚举值。
 *
 * @author Fu Wei
 */
@Getter
public enum DbType {

    MYSQL("MySQL", "jdbc:mysql://", "com.mysql.cj.jdbc.Driver", 3306),

    ORACLE("Oracle", "jdbc:oracle:thin:@", "oracle.jdbc.OracleDriver", 1521),

    POSTGRESQL("PostgreSQL", "jdbc:postgresql://", "org.postgresql.Driver", 5432),

    SQLSERVER("SQLServer", "jdbc:sqlserver://", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433),

    DAMENG("达梦数据库", "jdbc:dm://", "dm.jdbc.driver.DmDriver", 5236),

    KINGBASE("Kingbase", "jdbc:kingbase8://", "com.kingbase8.Driver", 54321),

    MONGODB("MongoDB", "mongodb://", null, 27017),

    CLICKHOUSE("ClickHouse", "jdbc:clickhouse://", "com.clickhouse.jdbc.ClickHouseDriver", 8123),

    OCEANBASE("OceanBase", "jdbc:oceanbase://", "com.oceanbase.jdbc.Driver", 2881),

    GAUSSDB("GaussDB", "jdbc:gaussdb://", "org.opengauss.Driver", 8000);

    /** 显示名称 */
    private final String displayName;

    /** JDBC URL 前缀（MongoDB 为连接串前缀，待实测确认） */
    private final String urlPrefix;

    /** 默认驱动类名（MongoDB 为 null，使用官方 sync driver） */
    private final String defaultDriverClass;

    /** 默认端口 */
    private final int defaultPort;

    DbType(String displayName, String urlPrefix, String defaultDriverClass, int defaultPort) {
        this.displayName = displayName;
        this.urlPrefix = urlPrefix;
        this.defaultDriverClass = defaultDriverClass;
        this.defaultPort = defaultPort;
    }

    /**
     * 判断该数据库类型是否为 JDBC 类型（走 JDBC 引擎）
     *
     * @return MongoDB 返回 false，其余返回 true
     */
    public boolean isJdbc() {
        return this != MONGODB;
    }

    /**
     * 判断该数据库类型是否为 OceanBase
     *
     * @return OceanBase 返回 true
     */
    public boolean isOceanBase() {
        return this == OCEANBASE;
    }
}
