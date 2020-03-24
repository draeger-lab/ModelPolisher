package edu.ucsd.sbrg.db;

import de.zbit.util.prefs.SBProperties;

public class DBConfig {

  public static void initADB(SBProperties args, boolean annotateWithADB) {
    if (annotateWithADB && ! AnnotateDB.inUse()) {
      initADB(args);
    }
  }


  /**
   * Sets DB to use, depending on provided arguments:
   * If annotateWithBigg is true and all arguments are provided, PostgreSQL is used
   *
   * @param args:
   *        Arguments from Commandline
   */
  private static void initADB(SBProperties args) {
    String name = args.getProperty(ADBOptions.DBNAME);
    String host = args.getProperty(ADBOptions.HOST);
    String passwd = args.getProperty(ADBOptions.PASSWD);
    String port = args.getProperty(ADBOptions.PORT);
    String user = args.getProperty(ADBOptions.USER);
    boolean run = iStrNotNullOrEmpty(name);
    run &= iStrNotNullOrEmpty(host);
    run &= iStrNotNullOrEmpty(port);
    run &= iStrNotNullOrEmpty(user);
    if (run) {
      AnnotateDB.init(host, port, user, passwd, name);
    }
  }


  public static void initBiGG(SBProperties args, boolean annotateWithBiGG) {
    if (annotateWithBiGG && !BiGGDB.inUse()) {
      initBiGG(args);
    }
  }


  /**
   * If annotateWithBigg is true and all arguments are provided, connection is established, else
   *
   * @param args:
   *        Arguments from Commandline
   */
  private static void initBiGG(SBProperties args) {
    String name = args.getProperty(BiGGDBOptions.DBNAME);
    String host = args.getProperty(BiGGDBOptions.HOST);
    String passwd = args.getProperty(BiGGDBOptions.PASSWD);
    String port = args.getProperty(BiGGDBOptions.PORT);
    String user = args.getProperty(BiGGDBOptions.USER);
    boolean run = iStrNotNullOrEmpty(name);
    run &= iStrNotNullOrEmpty(host);
    run &= iStrNotNullOrEmpty(port);
    run &= iStrNotNullOrEmpty(user);
    if (run) {
      BiGGDB.init(host, port, user, passwd, name);
    }
  }


  /**
   * @param string
   * @return
   */
  private static boolean iStrNotNullOrEmpty(String string) {
    return !(string == null || string.isEmpty());
  }
}
