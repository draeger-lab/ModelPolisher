/**
 * 
 */
package edu.ucsd.sbrg.bigg;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;

/**
 * @author Andreas Dr&auml;ger
 */
public interface BiGGDBOptions extends KeyProvider {

  /**
   * 
   */
  @SuppressWarnings("unchecked")
  public static final Option<String> BiGG_HOST = new Option<String>("BiGG_HOST", String.class, "Host name", "localhost");
  /**
   *
   */
  @SuppressWarnings("unchecked")
  public static final Option<Integer> BiGG_PORT = new Option<Integer>("BiGG_PORT", Integer.class, "Port", Integer.valueOf(5432)); // for MySQL it would be 3306
  /**
   *
   */
  @SuppressWarnings("unchecked")
  public static final Option<String> BiGG_USER = new Option<String>("BiGG_USER", String.class, "User name", System.getProperty("user.name"));
  /**
   *
   */
  public static final Option<String> BiGG_PASSWD = new Option<String>("BiGG_PASSWD", String.class, "Password");
  /**
   *
   */
  public static final Option<String> BiGG_DBNAME = new Option<String>("BiGG_DBNAME", String.class, "The name of the database to use.");

}
