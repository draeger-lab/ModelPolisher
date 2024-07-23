package edu.ucsd.sbrg.db.adb;

import de.zbit.util.Utils;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.db.PostgresConnectionPool;
import edu.ucsd.sbrg.parameters.DBParameters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.BIGG_METABOLITE;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.BIGG_REACTION;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.Column.NAMESPACE;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.Column.SOURCE_NAMESPACE;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.Column.SOURCE_TERM;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.Column.TARGET_NAMESPACE;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.Column.TARGET_TERM;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.Column.URLPATTERN;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.METABOLITE_PREFIX;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.REACTION_PREFIX;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.Table.ADB_COLLECTION;
import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.Table.MAPPING_VIEW;

/**
 * @author Kaustubh Trivedi
 */
public class AnnotateDB {

  private static final Logger logger = Logger.getLogger(AnnotateDB.class.getName());
  private static PostgresConnectionPool connectionPool;

  public AnnotateDB (DBParameters parameters) {
    String dbName = parameters.dbName();
    String host = parameters.host();
    String passwd = parameters.passwd();
    Integer port = parameters.port();
    String user = parameters.user();
    boolean run = iStrNotNullOrEmpty(dbName);
    run &= iStrNotNullOrEmpty(host);
    run &= port != null;
    run &= iStrNotNullOrEmpty(user);
    if (run) {
      init(host, port, user, passwd, dbName);
    }
  }

  private static boolean iStrNotNullOrEmpty(String string) {
    return !(string == null || string.isEmpty());
  }

  public AnnotateDB(String host, Integer port, String user, String passwd, String dbName)
  {
    init(host, port, user, passwd, dbName);
  }

  public static void init(String host, Integer port, String user, String passwd, String dbName) {
    if (null == connectionPool) {
      connectionPool = new PostgresConnectionPool(host, port, user, passwd, dbName);

      Runtime.getRuntime().addShutdownHook(new Thread(connectionPool::close));
    }
  }


  /**
   * Retrieves a set of annotated URLs based on the type and BiGG ID provided.
   * This method queries the database to find matching annotations and constructs URLs using the retrieved data.
   * 
   * @param type   The type of the BiGG ID, which can be either a metabolite or a reaction.
   * @param biggId The BiGG ID for which annotations are to be retrieved. The ID may be modified if it starts
   *               with specific prefixes or ends with an underscore.
   * @return A sorted set of URLs that are annotations for the given BiGG ID. If the type is neither metabolite
   *         nor reaction, or if an SQL exception occurs, an empty set is returned.
   */
  public Set<String> getAnnotations(String type, String biggId) {
    TreeSet<String> annotations = new TreeSet<>();
    // Check if the type is valid for querying annotations
    if (!type.equals(BIGG_METABOLITE) && !type.equals(BIGG_REACTION)) {
      return annotations;
    }
    // Adjust the BiGG ID if it starts with a known prefix
    if (type.equals(BIGG_METABOLITE) && biggId.startsWith(METABOLITE_PREFIX)) {
      biggId = biggId.substring(2);
    } else if (type.equals(BIGG_REACTION) && biggId.startsWith(REACTION_PREFIX)) {
      biggId = biggId.substring(2);
    }
    // Remove trailing underscore from the BiGG ID if present
    if (biggId.endsWith("_")) {
      biggId = biggId.substring(0, biggId.length() - 2);
    }
    // SQL query to fetch annotations
    String query = "SELECT m." + TARGET_TERM + ", ac." + URLPATTERN + " FROM " + MAPPING_VIEW + " m, " + ADB_COLLECTION
      + " ac WHERE m." + SOURCE_NAMESPACE + " = ? AND m." + SOURCE_TERM + " = ? AND ac." + NAMESPACE + " = m."
      + TARGET_NAMESPACE;
    try (Connection connection = connectionPool.getConnection();
         PreparedStatement pStatement = connection.prepareStatement(query)) {
      pStatement.setString(1, type);
      pStatement.setString(2, biggId);
      try (ResultSet resultSet = pStatement.executeQuery()) {
        // Process each result and construct the URL
        while (resultSet.next()) {
          String uri = resultSet.getString(URLPATTERN);
          String id = resultSet.getString(TARGET_TERM);
          uri = uri.replace("{$id}", id);
          annotations.add(uri);
        }
      }
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return annotations;
  }

}
