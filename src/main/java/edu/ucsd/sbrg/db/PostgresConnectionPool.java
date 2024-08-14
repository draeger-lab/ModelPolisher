package edu.ucsd.sbrg.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sbml.jsbml.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import static java.text.MessageFormat.format;

public class PostgresConnectionPool {

  private static final Logger logger = LoggerFactory.getLogger(PostgresConnectionPool.class);
  private final HikariDataSource dataSource;

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public void close() {
    dataSource.close();
  }

  public PostgresConnectionPool(String host, int port, String user, String password, String dbName) {
    password = password == null ? "" : password;
    Properties properties = new Properties();
    properties.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
    properties.setProperty("dataSource.user", user);
    properties.setProperty("dataSource.password", password);
    properties.setProperty("dataSource.databaseName", dbName);
    properties.setProperty("dataSource.serverName", host);
    properties.setProperty("dataSource.portNumber", Integer.toString(port));
    HikariConfig config = new HikariConfig(properties);
    config.setMaximumPoolSize(16);
    config.setReadOnly(true);
    dataSource = new HikariDataSource(config);
    logger.debug(format("{0}@{1}:{2}, password={3}", user, host, port, StringTools.fill(password.length(), '*')));
  }
}
