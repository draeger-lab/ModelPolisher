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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * PostgreSQL database helper class.
 * 
 * @author Andreas Dr&auml;ger
 */
public abstract class SQLConnector {

  protected static Connection connection;
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(SQLConnector.class.getName());


  /**
   * @throws SQLException
   */
  public void close() throws SQLException {
    if ((connection != null) && !connection.isClosed()) {
      connection.close();
      logger.fine(mpMessageBundle.getString("CONNECTION_CLOSED"));
    }
  }


  public abstract Connection connect() throws SQLException;


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


  /**
   * @return
   */
  public abstract String concat(String... strings);
}
