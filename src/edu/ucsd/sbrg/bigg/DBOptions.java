/**
 * 
 */
package edu.ucsd.sbrg.bigg;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;

/**
 * @author Andreas Dr&auml;ger
 */
public interface DBOptions extends KeyProvider {

  /**
   * 
   */
  public static final Option<String>  HOST   = new Option<String>("HOST", String.class, "Host name", "localhost");
  /**
   * 
   */
  public static final Option<Integer> PORT   = new Option<Integer>("PORT", Integer.class, "Port", Integer.valueOf(5432)); // for MySQL it would be 3306
  /**
   * 
   */
  public static final Option<String>  USER   = new Option<String>("USER", String.class, "User name", System.getProperty("user.name"));
  /**
   * 
   */
  public static final Option<String>  PASSWD = new Option<String>("PASSWD", String.class, "Password", "");
  /**
   * 
   */
  public static final Option<String>  DBNAME = new Option<String>("DBNAME", String.class, "The name of the database to use.");

}
