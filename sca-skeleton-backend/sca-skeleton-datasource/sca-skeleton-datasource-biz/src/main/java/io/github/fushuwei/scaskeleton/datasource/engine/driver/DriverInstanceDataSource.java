package io.github.fushuwei.scaskeleton.datasource.engine.driver;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 将 DriverInstance 包装为标准 DataSource。
 * <p>
 * 供 HikariCP 等连接池组件使用，底层通过 {@link DriverInstance#connect} 直接建立物理连接，
 * 解决类加载隔离导致的 DriverManager 无法发现驱动的问题。
 *
 * @author Fu Wei
 */
public class DriverInstanceDataSource implements DataSource {

    private final DriverInstance driverInstance;
    private final String url;
    private final Properties props;
    private PrintWriter logWriter;
    private int loginTimeout;

    public DriverInstanceDataSource(DriverInstance driverInstance, String url, Properties props) {
        this.driverInstance = driverInstance;
        this.url = url;
        this.props = props;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return driverInstance.connect(url, props);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Properties newProps = new Properties();
        if (props != null) {
            newProps.putAll(props);
        }
        if (username != null) {
            newProps.setProperty("user", username);
        }
        if (password != null) {
            newProps.setProperty("password", password);
        }
        return driverInstance.connect(url, newProps);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Cannot unwrap to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return loginTimeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("getParentLogger is not supported");
    }
}
