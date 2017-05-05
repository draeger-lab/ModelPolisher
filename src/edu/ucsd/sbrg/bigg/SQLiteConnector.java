package edu.ucsd.sbrg.bigg;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import edu.ucsd.sbrg.bigg.BiGGDBContract.Constants;

/**
 * Created by mephenor on 05.05.17.
 */
public class SQLiteConnector extends SQLConnector {

  /**
   *
   */
  private static final transient Logger logger =
    Logger.getLogger(PostgreSQLConnector.class.getName());


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
    connection = DriverManager.getConnection(
      "jdbc:sqlite::resource:edu/ucsd/sbrg/bigg/bigg.sqlite");
    config.apply(connection);
    logger.info(mpMessageBundle.getString("SQLITE_CONNECTED"));
    return connection;
  }

  /**
   * Workaround for SELECT CONCAT not being present in sqlite
   * @return
   */
  public String selectConcat() {
    return "SELECT (" + Constants.URL_PREFIX + "|| s." + Constants.SYNONYM
      + ")";
  }


  /**
   * @throws ClassNotFoundException
   */
  public SQLiteConnector() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
  }
}
