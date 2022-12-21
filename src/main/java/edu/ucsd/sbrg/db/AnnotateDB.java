package edu.ucsd.sbrg.db;

import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.ADB_COLLECTION;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_NAMESPACE;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_SOURCE_NAMESPACE;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_SOURCE_TERM;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_TARGET_NAMESPACE;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_TARGET_TERM;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.COLUMN_URLPATTERN;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.MAPPING_VIEW;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
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
  private static PostgreSQLConnector connector;

  /**
   * Don't allow instantiation
   */
  private AnnotateDB() {
  }


  /**
   * Initialize a SQL connection
   * 
   * @param host
   * @param port
   * @param user
   * @param passwd
   * @param name
   */
  public static void init(String host, String port, String user, String passwd, String name) {
    connector = new PostgreSQLConnector(host, Integer.parseInt(port), user, passwd, name);
  }


  /**
   * 
   */
  public static void close() {
    connector.close();
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
  public static Set<String> getAnnotations(String type, String biggId) {
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
      + ADB_COLLECTION + " ac" + WHERE + "m." + COLUMN_SOURCE_NAMESPACE + " = ? AND " + "m." + COLUMN_SOURCE_TERM
      + " = ? AND ac." + COLUMN_NAMESPACE + " = m." + COLUMN_TARGET_NAMESPACE;
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, type);
      pStatement.setString(2, biggId);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        String uri = resultSet.getString(COLUMN_URLPATTERN);
        String id = resultSet.getString(COLUMN_TARGET_TERM);
        uri = uri.replace("{$id}", id);
        annotations.add(uri);
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return annotations;
  }
}
