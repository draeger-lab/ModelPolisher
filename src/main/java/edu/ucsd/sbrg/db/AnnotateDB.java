package edu.ucsd.sbrg.db;

import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.ADB_COLLECTION;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_NAMESPACE;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_SOURCE_NAMESPACE;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_SOURCE_TERM;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_TARGET_NAMESPACE;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_TARGET_TERM;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_URLPATTERN;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.MAPPING_VIEW;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.logging.Logger;

import de.zbit.util.Utils;

/**
 * @author Kaustubh Trivedi
 */
public class AnnotateDB {

  private static final Logger logger = Logger.getLogger(AnnotateDB.class.getName());
  private static final String SELECT = "SELECT ";
  private static final String FROM = " FROM ";
  private static final String WHERE = " WHERE ";
  // source_namespace types:
  public static final String BIGG_METABOLITE = "bigg.metabolite";
  public static final String BIGG_REACTION = "bigg.reaction";
  // prefixes
  static final String METABOLITE_PREFIX = "M_";
  static final String REACTION_PREFIX = "R_";
  static final String GENE_PREFIX = "G_";
  private static SQLConnector connector;

  /**
   * Don't allow instantiation
   */
  private AnnotateDB() {
  };


  /**
   * Initialize a SQL connection
   * 
   * @param host
   * @param port
   * @param user
   * @param passwd
   * @param name
   * @throws ClassNotFoundException
   */
  public static void init(String host, String port, String user, String passwd, String name)
    throws ClassNotFoundException {
    connector = new PostgreSQLConnector(host, Integer.parseInt(port), user, passwd != null ? passwd : "", name);
  }


  /**
   *
   */
  public static void startConnection() throws SQLException {
    if (!connector.isConnected()) {
      connector.connect();
    }
  }


  /**
   *
   */
  public static void closeConnection() {
    if (connector.isConnected()) {
      try {
        connector.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }


  /**
   * @return
   */
  public static boolean inUse() {
    return connector != null;
  }


  /**
   * @param type
   * @param biggId
   * @return
   */
  public static TreeSet<String> getAnnotations(String type, String biggId) {
    TreeSet<String> annotations = new TreeSet<>();
    if (!type.equals(BIGG_METABOLITE) && !type.equals(BIGG_REACTION)) {
      return annotations;
    }
    if (type.equals(BIGG_METABOLITE) && biggId.startsWith(METABOLITE_PREFIX)) {
      biggId = biggId.substring(2);
    } else if (type.equals(BIGG_METABOLITE) && biggId.startsWith(REACTION_PREFIX)) {
      biggId = biggId.substring(2);
    }
    if (biggId.endsWith("_")) {
      biggId = biggId.substring(0, biggId.length() - 2);
    }
    String query = SELECT + "m." + COLUMN_TARGET_TERM + ", ac." + COLUMN_URLPATTERN + FROM + MAPPING_VIEW + " m, "
      + ADB_COLLECTION + " ac" + WHERE + "m." + COLUMN_SOURCE_NAMESPACE + " = '" + type + "' AND " + "m."
      + COLUMN_SOURCE_TERM + " = '" + biggId + "' AND ac." + COLUMN_NAMESPACE + " = m." + COLUMN_TARGET_NAMESPACE;
    try {
      startConnection();
      ResultSet rst = connector.query(query);
      while (rst.next()) {
        String uri = rst.getString(COLUMN_URLPATTERN);
        String id = rst.getString(COLUMN_TARGET_TERM);
        uri = uri.replace("{$id}", id);
        annotations.add(uri);
      }
      closeConnection();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return annotations;
  }
}
