package edu.ucsd.sbrg.db;

import java.sql.SQLException;

import de.zbit.util.prefs.SBProperties;

public class DBConfig {

  public static AnnotateDB getADB(SBProperties args, boolean annotateWithADB) {
    if (annotateWithADB) {
      return getADB(args);
    }
    return null;
  }


  /**
   * Sets DB to use, depending on provided arguments:
   * If annotateWithBigg is true and all arguments are provided, PostgreSQL is used
   *
   * @param args:
   *        Arguments from Commandline
   */
  private static AnnotateDB getADB(SBProperties args) {
    String name = args.getProperty(ADBOptions.DBNAME);
    String host = args.getProperty(ADBOptions.HOST);
    String passwd = args.getProperty(ADBOptions.PASSWD);
    String port = args.getProperty(ADBOptions.PORT);
    String user = args.getProperty(ADBOptions.USER);
    boolean run = iStrNotNullOrEmpty(name);
    run &= iStrNotNullOrEmpty(host);
    run &= iStrNotNullOrEmpty(port);
    run &= iStrNotNullOrEmpty(user);
    AnnotateDB adb = null;
    if (run) {
      try {
        adb = new AnnotateDB(
          new PostgreSQLConnector(host, Integer.parseInt(port), user, passwd != null ? passwd : "", name));
      } catch (ClassNotFoundException exc) {
        exc.printStackTrace();
        System.exit(1);
      }
    }
    return adb;
  }


  public static BiGGDB getBiGG(SBProperties args, boolean annotateWithBiGG) {
    if (annotateWithBiGG) {
      return getBiGG(args);
    }
    return null;
  }


  /**
   * If annotateWithBigg is true and all arguments are provided, connection is established, else
   *
   * @param args:
   *        Arguments from Commandline
   */
  private static BiGGDB getBiGG(SBProperties args) {
    String name = args.getProperty(BiGGDBOptions.DBNAME);
    String host = args.getProperty(BiGGDBOptions.HOST);
    String passwd = args.getProperty(BiGGDBOptions.PASSWD);
    String port = args.getProperty(BiGGDBOptions.PORT);
    String user = args.getProperty(BiGGDBOptions.USER);
    boolean run = iStrNotNullOrEmpty(name);
    run &= iStrNotNullOrEmpty(host);
    run &= iStrNotNullOrEmpty(port);
    run &= iStrNotNullOrEmpty(user);
    BiGGDB bigg = null;
    if (run) {
      try {
        bigg =
          new BiGGDB(new PostgreSQLConnector(host, Integer.parseInt(port), user, passwd != null ? passwd : "", name));
      } catch (SQLException | ClassNotFoundException exc) {
        exc.printStackTrace();
        System.exit(1);
      }
    }
    return bigg;
  }


  /**
   * @param string
   * @return
   */
  private static boolean iStrNotNullOrEmpty(String string) {
    return !(string == null || string.isEmpty());
  }
}
