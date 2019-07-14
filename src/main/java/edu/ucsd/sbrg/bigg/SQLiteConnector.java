package main.java.edu.ucsd.sbrg.bigg;

import static main.java.edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

/**
 * Created by mephenor on 05.05.17.
 */
public class SQLiteConnector extends SQLConnector {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(SQLiteConnector.class.getName());


  /**
   * @return
   * @throws SQLException
   */
  @Override
  public Connection connect() throws SQLException {
    if (isConnected()) {
      connection.close();
    }
    SQLiteConfig config = new SQLiteConfig();
    config.setOpenMode(SQLiteOpenMode.OPEN_MEMORY);
    connection = DriverManager.getConnection("jdbc:sqlite::resource:edu/ucsd/sbrg/bigg/bigg.sqlite");
    config.apply(connection);
    logger.info(mpMessageBundle.getString("SQLITE_CONNECTED"));
    return connection;
  }


  // Workaround for SELECT CONCAT not being present in sqlite
  @Override
  public String concat(String... strings) {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    for (int i = 0; i < strings.length; i++) {
      if (i > 0) {
        sb.append("|| ");
      }
      sb.append(strings[i]);
    }
    sb.append(')');
    return sb.toString();
  }


  /**
   * @throws ClassNotFoundException
   */
  SQLiteConnector() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
  }
}
