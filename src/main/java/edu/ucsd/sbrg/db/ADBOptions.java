package edu.ucsd.sbrg.db;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;

/**
 * @author Kaustubh Trivedi
 */
public interface ADBOptions extends KeyProvider {

  /**
   *
   */
  @SuppressWarnings("unchecked")
  Option<String> HOST = new Option<>("ADB_HOST", String.class, "Host name", "adb");
  /**
   *
   */
  @SuppressWarnings("unchecked")
  Option<Integer> PORT = new Option<>("ADB_PORT", Integer.class, "Port", 1013);
  /**
   *
   */
  @SuppressWarnings("unchecked")
  Option<String> USER = new Option<>("ADB_USER", String.class, "User name", "postgres");
  /**
   *
   */
  Option<String> PASSWD = new Option<>("ADB_PASSWD", String.class, "postgres");
  /**
   *
   */
  @SuppressWarnings("unchecked")
  Option<String> DBNAME = new Option<>("ADB_DBNAME", String.class, "The name of the database to use.", "adb");
}
