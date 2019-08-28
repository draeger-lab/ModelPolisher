package edu.ucsd.sbrg.bigg;

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
    public static final Option<String> ADB_HOST   = new Option<String>("ADB_HOST", String.class, "Host name", "adb");
    /**
     *
     */
    @SuppressWarnings("unchecked")
    public static final Option<Integer> ADB_PORT   = new Option<Integer>("ADB_PORT", Integer.class, "Port", Integer.valueOf(5432)); // for MySQL it would be 3306
    /**
     *
     */
    @SuppressWarnings("unchecked")
    public static final Option<String>  ADB_USER   = new Option<String>("ADB_USER", String.class, "User name", "postgres");
    /**
     *
     */
    public static final Option<String>  ADB_PASSWD = new Option<String>("ADB_PASSWD", String.class, "postgres");
    /**
     *
     */
    public static final Option<String>  ADB_DBNAME = new Option<String>("ADB_DBNAME", String.class, "The name of the database to use.","adb");

}
