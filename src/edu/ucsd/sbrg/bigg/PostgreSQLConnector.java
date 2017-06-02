package edu.ucsd.sbrg.bigg;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import org.sbml.jsbml.util.StringTools;

/**
 * Created by mephenor on 05.05.17.
 */
public class PostgreSQLConnector extends SQLConnector {

  private enum Keys {
    /**
     *
     */
    databaseName,
    /**
     *
     */
    dbms,
    /**
     *
     */
    host,
    /**
     *
     */
    password,
    /**
     *
     */
    portNumber,
    /**
     *
     */
    server_name,
    /**
     *
     */
    user
  }

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger =
      Logger.getLogger(PostgreSQLConnector.class.getName());
  /**
   *
   */
  private Properties properties;


  /**
   * @return
   * @throws SQLException
   */
  @Override
  public Connection connect() throws SQLException {
    if (isConnected()) {
      connection.close();
    }
    String url = "jdbc:" + properties.getProperty(Keys.dbms.toString()) + "://"
        + getHost() + ":" + getPort() + "/" + getDatabaseName();
    connection = DriverManager.getConnection(url, properties);
    connection.setCatalog(properties.getProperty(Keys.databaseName.toString()));
    logger.info(format(mpMessageBundle.getString("PSQL_CONNECTED"), getHost(),
      getPort(), getDatabaseName()));
    return connection;
  }


  /**
   * @return
   */
  public String getDatabaseName() {
    return getProperty(Keys.databaseName);
  }


  /**
   * @return
   */
  public String getHost() {
    return getProperty(Keys.host);
  }


  /**
   * @return
   */
  public int getPort() {
    String port = getProperty(Keys.portNumber);
    return (port != null) ? Integer.parseInt(port) : -1;
  }


  /**
   * @return the properties
   */
  protected Properties getProperties() {
    return properties;
  }


  /**
   * @param key
   * @return
   */
  private String getProperty(Keys key) {
    return properties != null ? properties.getProperty(key.toString()) : null;
  }


  // Might not be present in other db formats, so make it PSQL specific
  @Override
  public String concat(String... strings) {
    StringBuilder sb = new StringBuilder();
    sb.append("CONCAT(");
    for (int i = 0; i < strings.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(strings[i]);
    }
    sb.append(')');
    return sb.toString();
  }


  /**
   * @return
   */
  public String getUser() {
    return getProperty(Keys.user);
  }


  /**
   * @param host
   * @param port
   * @param user
   * @param password
   * @param dbName
   * @throws ClassNotFoundException
   */
  public PostgreSQLConnector(String host, int port, String user,
    String password, String dbName) throws ClassNotFoundException {
    Class.forName("org.postgresql.Driver");
    properties = new Properties();
    properties.setProperty(Keys.dbms.toString(), "postgresql");
    properties.setProperty(Keys.host.toString(), host);
    properties.setProperty(Keys.user.toString(), user);
    properties.setProperty(Keys.databaseName.toString(), dbName);
    properties.setProperty(Keys.password.toString(),
      password != null ? password : "");
    properties.setProperty(Keys.host.toString(), host);
    properties.setProperty(Keys.portNumber.toString(), Integer.toString(port));
    logger.fine(format("{0}@{1}:{2}, password={3}", user, host, port,
      StringTools.fill(password.length(), '*')));
  }

}
