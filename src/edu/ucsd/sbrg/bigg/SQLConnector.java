/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the program NetBuilder.
 * Copyright (C) 2013 by the University of California, San Diego.
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package edu.ucsd.sbrg.bigg;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

import org.sbml.jsbml.util.StringTools;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

/**
 * PostgreSQL database helper class.
 * 
 * @author Andreas Dr&auml;ger
 */
public class SQLConnector {

  /**
   * A {@link Logger} for this class.
   */
  private static Logger logger =
    Logger.getLogger(PostgreSQLConnector.class.getName());
  /**
   *
   */
  private static Connection connection;

  /**
   * @author Andreas Dr&auml;ger
   */
  private static enum Keys {
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
                            user;
  }

  public class PostgreSQLConnector extends SQLConnector {

    /**
     *
     */
    private Properties properties;


    /**
     * @return
     * @throws SQLException
     */
    public Connection connect() throws SQLException {
      if (isConnected()) {
        connection.close();
      }
      String url = "jdbc:" + properties.getProperty(Keys.dbms.toString())
        + "://" + getHost() + ":" + getPort() + "/" + getDatabaseName();
      connection = DriverManager.getConnection(url, properties);
      connection.setCatalog(
        properties.getProperty(Keys.databaseName.toString()));
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
      properties.setProperty(Keys.portNumber.toString(),
        Integer.toString(port));
      logger.fine(format("{0}@{1}:{2}, password={3}", user, host, port,
        StringTools.fill(password.length(), '*')));
    }
  }

  public class SQLiteConnector extends SQLConnector {

    /**
     * @return
     * @throws SQLException
     */
    public Connection connect() throws SQLException {
      if (isConnected()) {
        connection.close();
      }
      SQLiteConfig config = new SQLiteConfig();
      config.setOpenMode(SQLiteOpenMode.OPEN_MEMORY);
      connection =
        DriverManager.getConnection("jdbc:sqlite::resource:bigg.sqlite");
      config.apply(connection);
      logger.info(mpMessageBundle.getString("SQLITE_CONNECTED"));
      return connection;
    }


    /**
     * @throws ClassNotFoundException
     */
    public SQLiteConnector() throws ClassNotFoundException {
      Class.forName("org.sqlite.JDBC");
    }
  }


  /**
   * @throws SQLException
   */
  public void close() throws SQLException {
    if ((connection != null) && !connection.isClosed()) {
      connection.close();
      logger.fine(mpMessageBundle.getString("CONNECTION_CLOSED"));
    }
  }


  /*
   * (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  @Override
  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }


  /**
   * @return the connection
   */
  public Connection getConnection() {
    return connection;
  }


  /**
   * @return
   */
  public boolean isConnected() {
    try {
      return (connection != null) && !connection.isClosed();
    } catch (SQLException exc) {
      return false;
    }
  }


  /**
   * @param query
   * @param args
   * @return
   * @throws SQLException
   */
  public ResultSet query(String query, Object... args) throws SQLException {
    Statement stmt = connection.createStatement();
    return stmt.executeQuery(String.format(query, args));
  }
}
