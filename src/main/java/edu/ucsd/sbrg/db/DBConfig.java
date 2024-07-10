package edu.ucsd.sbrg.db;

import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.db.adb.AnnotateDBOptions;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.db.bigg.BiGGDBOptions;


/**
 * This class provides configuration and initialization methods for database connections.
 * It supports operations for two specific databases: ADB and BiGG.
 * It includes methods to initialize these databases based on command line arguments
 * and to check if the necessary parameters are provided before establishing connections.
 */
public class DBConfig {

  /**
   * Initializes the ADB database if the conditions are met.
   * It checks if annotation with ADB is required and if ADB is not already in use.
   *
   * @param args            Command line arguments provided for database configuration.
   * @param annotateWithADB Flag indicating whether to annotate with ADB.
   */
  public static void initADB(SBProperties args, boolean annotateWithADB) {
    if (annotateWithADB && !AnnotateDB.inUse()) {
      initADB(args);
    }
  }

  /**
   * Private helper method to initialize the ADB database connection.
   * It retrieves database connection parameters from the provided arguments and initializes the connection if all parameters are valid.
   *
   * @param args Command line arguments containing database connection parameters.
   */
  private static void initADB(SBProperties args) {
    String name = args.getProperty(AnnotateDBOptions.DBNAME);
    String host = args.getProperty(AnnotateDBOptions.HOST);
    String passwd = args.getProperty(AnnotateDBOptions.PASSWD);
    String port = args.getProperty(AnnotateDBOptions.PORT);
    String user = args.getProperty(AnnotateDBOptions.USER);
    boolean run = iStrNotNullOrEmpty(name);
    run &= iStrNotNullOrEmpty(host);
    run &= iStrNotNullOrEmpty(port);
    run &= iStrNotNullOrEmpty(user);
    if (run) {
      AnnotateDB.init(host, port, user, passwd, name);
    }
  }

  /**
   * Initializes the BiGG database if the conditions are met.
   * It checks if annotation with BiGG is required and if BiGG is not already in use.
   *
   * @param args             Command line arguments provided for database configuration.
   * @param annotateWithBiGG Flag indicating whether to annotate with BiGG.
   */
  public static void initBiGG(SBProperties args, boolean annotateWithBiGG) {
    if (annotateWithBiGG && !BiGGDB.inUse()) {
      initBiGG(args);
    }
  }

  /**
   * Private helper method to initialize the BiGG database connection.
   * It retrieves database connection parameters from the provided arguments and initializes the connection if all parameters are valid.
   *
   * @param args Command line arguments containing database connection parameters.
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
   * Utility method to check if a string is neither null nor empty.
   *
   * @param string The string to check.
   * @return true if the string is not null and not empty, false otherwise.
   */
  private static boolean iStrNotNullOrEmpty(String string) {
    return !(string == null || string.isEmpty());
  }
}
