/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the program NetBuilder.
 * Copyright (C) 2013 by the University of California, San Diego.
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package edu.ucsd.sbrg.db;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.*;
import static java.text.MessageFormat.format;
import static org.sbml.jsbml.util.Pair.pairOf;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.logging.Logger;

import org.sbml.jsbml.util.Pair;

import de.zbit.util.Utils;
import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.miriam.Registry;

/**
 * @author Andreas Dr&auml;ger
 */
public class BiGGDB {

  private static final String SELECT = "SELECT ";
  private static final String FROM = " FROM ";
  private static final String WHERE = " WHERE ";
  public static final String TYPE_SPECIES = "SPECIES";
  public static final String TYPE_REACTION = "REACTION";
  public static final String TYPE_GENE_PRODUCT = "GENE_PRODUCT";
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(BiGGDB.class.getName());
  /**
   * The connection to the database.
   */
  private static PostgreSQLConnector connector;

  /**
   * Don't allow instantiation
   */
  private BiGGDB() {
  };


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
   * @return
   */
  public static Date getBiGGVersion() {
    Date date = null;
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement("Select date_time FROM database_version");
      ResultSet resultSet = pStatement.executeQuery();
      if (resultSet.next()) {
        date = resultSet.getDate(1);
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.finest(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    return date;
  }


  /**
   * @param modelBiGGid
   * @param reactionBiGGid
   * @return
   */
  public static List<String> getSubsystems(String modelBiGGid, String reactionBiGGid) {
    String query = "SELECT DISTINCT mr.subsystem FROM reaction r, model m, model_reaction mr WHERE m.bigg_id = ? "
      + "AND r.bigg_id = ? AND m.id = mr.model_id AND r.id = mr.reaction_id AND LENGTH(mr.subsystem) > 0";
    List<String> list = new LinkedList<>();
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, modelBiGGid);
      pStatement.setString(2, reactionBiGGid);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        list.add(resultSet.getString(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return list;
  }


  /**
   * Get chemical formula for unknown model id
   *
   * @param componentId
   * @param compartmentId
   * @return
   */
  public static String getChemicalFormulaByCompartment(String componentId, String compartmentId) {
    String result = "";
    String query = "SELECT DISTINCT mcc." + COLUMN_FORMULA + " FROM " + MCC + " mcc, " + COMPARTMENTALIZED_COMPONENT
      + " cc, " + COMPONENT + " c, " + COMPARTMENT + " co WHERE c." + COLUMN_BIGG_ID + " = ? AND c." + COLUMN_ID
      + " = cc." + COLUMN_COMPONENT_ID + " AND co." + COLUMN_BIGG_ID + " = ? AND co." + COLUMN_ID + " = cc."
      + COLUMN_COMPARTMENT_ID + " and cc." + COLUMN_ID + " = mcc." + COLUMN_COMPARTMENTALIZED_COMPONENT_ID
      + " AND LENGTH(mcc." + COLUMN_FORMULA + ") > 0 ORDER BY mcc." + COLUMN_FORMULA;
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, componentId);
      pStatement.setString(2, compartmentId);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        result = resultSet.getString(1);
        if (result != null && !result.trim().isEmpty()) {
          break;
        }
      }
      while (resultSet.next()) {
        String tmp = resultSet.getString(1);
        if (tmp != null && !tmp.trim().isEmpty()) {
          logger.info(format(mpMessageBundle.getString("RST_NOT_UNIQUE"), "formula", componentId, compartmentId));
          result = "";
        }
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return result;
  }


  /**
   * Get chemical formula for models that are present in BiGG
   *
   * @param abbreviation
   * @param modelId
   * @return
   */
  public static String getChemicalFormula(String abbreviation, String modelId) {
    String result = "";
    String query = "SELECT DISTINCT mcc." + COLUMN_FORMULA + "\n FROM " + COMPONENT + " c,\n"
      + COMPARTMENTALIZED_COMPONENT + " cc,\n" + MODEL + " m,\n" + MCC + " mcc\n WHERE c." + COLUMN_ID + " = cc."
      + COLUMN_COMPONENT_ID + " AND\n cc." + COLUMN_ID + " = mcc." + COLUMN_COMPARTMENTALIZED_COMPONENT_ID + " AND\n c."
      + COLUMN_BIGG_ID + " = ? AND\n m." + COLUMN_BIGG_ID + " = ? AND\n m." + COLUMN_ID + " = mcc." + COLUMN_MODEL_ID
      + " AND mcc.formula <> ''";
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, abbreviation);
      pStatement.setString(2, modelId);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        if (!result.isEmpty()) {
          throw new IllegalStateException("Chemical formula query returned multiple results:\n" + query);
        } else {
          result = resultSet.getString(1);
        }
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return result == null ? "" : result;
  }


  /**
   * @param biggId
   * @return
   */
  public static String getCompartmentName(BiGGId biggId) {
    String query = "SELECT name FROM compartment WHERE bigg_id = ? AND name <> ''";
    return singleParamStatement(query, biggId.getAbbreviation());
  }


  /**
   * @param query
   * @param param
   * @return
   */
  public static String singleParamStatement(String query, String param) {
    String result = "";
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, param);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        if (!result.isEmpty()) {
          logger.severe("Query returned multiple results:\n" + query);
        }
        result = resultSet.getString(1);
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return result;
  }


  /**
   * @param biggId
   * @return
   */
  public static String getComponentName(BiGGId biggId) {
    String query = "SELECT name FROM component WHERE bigg_id = ? AND name <> ''";
    return singleParamStatement(query, biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   */
  public static String getComponentType(BiGGId biggId) {
    String query = "SELECT type FROM component WHERE bigg_id = ? AND name <> ''";
    return singleParamStatement(query, biggId.getAbbreviation());
  }


  /**
   * Here we get all possible MIRIAM annotation for Gene Labels, but we ignore
   * all those entries that are not MIRIAM-compliant for now.
   *
   * @param label
   * @return
   */
  public static TreeSet<String> getGeneIds(String label) {
    TreeSet<String> set = new TreeSet<>();
    String query = SELECT + URL_PREFIX + ", s." + SYNONYM + "\n" + "FROM  " + DATA_SOURCE + " d, " + SYNONYM + " s, "
      + GENOME_REGION + " gr\n" + "WHERE d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND\n s." + COLUMN_OME_ID
      + " = gr." + COLUMN_ID + " AND\n gr." + COLUMN_BIGG_ID + " = ? AND\n d." + COLUMN_BIGG_ID + " != " + OLD_BIGG_ID
      + " AND\n d." + COLUMN_BIGG_ID + " NOT LIKE " + REFSEQ_PATTERN;
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, label);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        String resource;
        String collection = resultSet.getString(1);
        String identifier = resultSet.getString(2);
        if (collection != null && identifier != null) {
          resource = collection + identifier;
        } else if (collection == null) {
          logger.fine(mpMessageBundle.getString("COLLECTION_NULL_GENE"));
          continue;
        } else {
          logger.warning(format(mpMessageBundle.getString("IDENTIFIER_NULL_GENE"), collection));
          continue;
        }
        if ((resource = Registry.checkResourceUrl(resource)) != null) {
          set.add(resource);
        }
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return set;
  }


  /**
   * @param label
   * @return
   */
  public static String getGeneName(String label) {
    String query = "SELECT s." + SYNONYM + "\n" + "FROM  " + DATA_SOURCE + " d, " + SYNONYM + " s, " + GENOME_REGION
      + " gr\n" + "WHERE d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND\n s." + COLUMN_OME_ID + " = gr."
      + COLUMN_ID + " AND\n gr." + COLUMN_BIGG_ID + " = ? AND\n d." + COLUMN_BIGG_ID + " LIKE " + REFSEQ_NAME
      + " AND s.synonym <> ''";
    return singleParamStatement(query, label);
  }


  /**
   * @param reactionId
   * @param modelId
   * @return
   */
  public static List<String> getGeneReactionRule(String reactionId, String modelId) {
    return getReactionRules("SELECT REPLACE(RTRIM(REPLACE(REPLACE(mr." + COLUMN_GENE_REACTION_RULE
      + ", 'or', '||'), 'and', '&&'), '.'), '.', '__SBML_DOT__') AS " + COLUMN_GENE_REACTION_RULE + " FROM "
      + MODEL_REACTION + " mr, " + REACTION + " r, " + MODEL + " m WHERE r." + COLUMN_ID + " = mr." + COLUMN_REACTION_ID
      + " AND m." + COLUMN_ID + " = mr." + COLUMN_MODEL_ID + " AND mr." + COLUMN_GENE_REACTION_RULE
      + " IS NOT NULL AND  LENGTH(mr." + COLUMN_GENE_REACTION_RULE + ") > 0 AND r." + COLUMN_BIGG_ID + " = ? AND m."
      + COLUMN_BIGG_ID + " = ? AND mr.gene_reaction_rule <> '' ORDER BY mr." + COLUMN_ID, reactionId, modelId);
  }


  /**
   * @param query
   * @param reactionId
   * @param modelId
   * @return
   */
  public static List<String> getReactionRules(String query, String reactionId, String modelId) {
    List<String> result = new ArrayList<>();
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, reactionId);
      pStatement.setString(2, modelId);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        result.add(resultSet.getString(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return result;
  }


  /**
   * @param abbreviation
   * @return
   */
  public static String getOrganism(String abbreviation) {
    String query = "SELECT g." + COLUMN_ORGANISM + FROM + GENOME + " g, " + MODEL + " m WHERE m." + COLUMN_GENOME_ID
      + " = g." + COLUMN_ID + " AND m." + COLUMN_BIGG_ID + " = ?";
    return singleParamStatement(query, abbreviation);
  }


  /**
   * @param abbreviation
   * @return
   */
  public static List<Pair<String, String>> getPublications(String abbreviation) {
    List<Pair<String, String>> result = new LinkedList<>();
    String query = "SELECT p." + COLUMN_REFERENCE_TYPE + ", p." + COLUMN_REFERENCE_ID + " FROM  " + PUBLICATION + " p, "
      + PUBLICATION_MODEL + " pm, " + MODEL + " m WHERE p." + COLUMN_ID + " = pm." + COLUMN_PUBLICATION_ID + " AND pm."
      + COLUMN_MODEL_ID + " = m." + COLUMN_ID + " AND m." + COLUMN_BIGG_ID + " = ?";
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, abbreviation);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        String key = resultSet.getString(1);
        result.add(pairOf(key.equals("pmid") ? "pubmed" : key, resultSet.getString(2)));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return result;
  }


  /**
   * @param abbreviation
   * @return
   */
  public static String getReactionName(String abbreviation) {
    String query = "SELECT name FROM reaction WHERE bigg_id = ? AND name <> ''";
    return singleParamStatement(query, abbreviation);
  }


  /**
   * @param biggId
   * @param includeAnyURI
   * @param isReaction
   * @return a set of external source together with external id.
   * @throws SQLException
   */
  public static Set<String> getResources(BiGGId biggId, boolean includeAnyURI, boolean isReaction) {
    String type = isReaction ? REACTION : COMPONENT;
    Set<String> resources = new TreeSet<>();
    try {
      String query = String.format(
        "SELECT CONCAT(url_prefix, s.synonym) AS url FROM %s t, synonym s, data_source d WHERE t.id = s.ome_id AND s.data_source_id = d.id AND url_prefix IS NOT NULL AND %s AND t.bigg_id = ? %s",
        type, getTypeQuery(isReaction), includeAnyURI ? "" : "AND url_prefix like '%%identifiers.org%%'");
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, biggId.getAbbreviation());
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        String resource = resultSet.getString(1);
        resource = Registry.checkResourceUrl(resource);
        if (resource != null) {
          resources.add(resource);
        }
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return resources;
  }


  /**
   * @param isReaction
   * @return
   */
  private static String getTypeQuery(boolean isReaction) {
    if (isReaction) {
      return "CAST(s.type AS \"text\") = '" + REACTION + "'";
    }
    return "(CAST(s.type AS \"text\") = '" + COMPONENT + "' OR CAST(s.type AS \"text\") = '"
      + COMPARTMENTALIZED_COMPONENT + "')";
  }


  /**
   * @param abbreviation
   * @return
   */
  public static int getTaxonId(String abbreviation) {
    int result = Integer.MIN_VALUE;
    String query = SELECT + COLUMN_TAXON_ID + FROM + GENOME + " g, " + MODEL + " m WHERE g." + COLUMN_ID + " = m."
      + COLUMN_GENOME_ID + " AND m." + COLUMN_BIGG_ID + " = ? AND taxon_id IS NOT NULL";
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, abbreviation);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        if (result > Integer.MIN_VALUE) {
          logger.severe("Taxon id query returned multiple results:\n" + query);
        } else {
        result = resultSet.getInt(1);}
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(format(mpMessageBundle.getString("GET_TAXON_ERROR"), abbreviation, Utils.getMessage(exc)));
    }
    return result;
  }


  public static Set<String> getOnce(String table) {
    Set<String> biggIds = new LinkedHashSet<>();
    try {
      Connection connection = connector.getConnection();
      String query = "SELECT bigg_id FROM " + table + " ORDER BY bigg_id";
      PreparedStatement pStatement = connection.prepareStatement(query);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        biggIds.add(resultSet.getString(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(String.format("Failed to fetch BiGGIDs for table '%s': '%s'", table, Utils.getMessage(exc)));
    }
    return biggIds;
  }


  /**
   * Get charge for unknown model id
   *
   * @param componentId
   * @param compartmentId
   * @return
   */
  public static int getChargeByCompartment(String componentId, String compartmentId) {
    String query = "SELECT DISTINCT mcc." + COLUMN_CHARGE + " FROM " + MCC + " mcc, " + COMPARTMENTALIZED_COMPONENT
      + " cc, " + COMPONENT + " c, " + COMPARTMENT + " co WHERE c." + COLUMN_BIGG_ID + " = ? AND c." + COLUMN_ID
      + " = cc." + COLUMN_COMPONENT_ID + " AND co." + COLUMN_BIGG_ID + " = ? AND co." + COLUMN_ID + " = cc."
      + COLUMN_COMPARTMENT_ID + " and cc." + COLUMN_ID + " = mcc." + COLUMN_COMPARTMENTALIZED_COMPONENT_ID
      + " AND LENGTH(CAST( mcc." + COLUMN_CHARGE + " AS text)) > 0 ORDER BY mcc." + COLUMN_CHARGE;
    String charge = "";
    boolean unique = true;
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, componentId);
      pStatement.setString(2, compartmentId);
      ResultSet resultSet = pStatement.executeQuery();
      String result = "";
      if (resultSet.isBeforeFirst()) {
        while (resultSet.next()) {
          result = resultSet.getString(1);
          if (result != null && !result.trim().isEmpty()) {
            break;
          }
        }
      }
      while (resultSet.next()) {
        String tmp = resultSet.getString(1);
        if (tmp != null && !tmp.trim().isEmpty()) {
          logger.info(format(mpMessageBundle.getString("RST_NOT_UNIQUE"), "charge", componentId, compartmentId));
          unique = false;
        }
      }
      pStatement.close();
      connection.close();
      if (unique) {
        charge = result;
      }
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    if (charge == null || charge.trim().length() == 0) {
      return Integer.MIN_VALUE;
    }
    return Integer.parseInt(charge);
  }


  /**
   * Get charge for known model id
   *
   * @param abbreviation
   * @param modelId
   * @return
   */
  public static int getCharge(String abbreviation, String modelId) {
    int result = Integer.MIN_VALUE;
    String query =
      "SELECT DISTINCT mcc." + COLUMN_CHARGE + "\n FROM " + COMPONENT + " c,\n" + COMPARTMENTALIZED_COMPONENT + " cc,\n"
        + MODEL + " m,\n" + MCC + " mcc\n WHERE c." + COLUMN_ID + " = cc." + COLUMN_COMPONENT_ID + " AND\n cc."
        + COLUMN_ID + " = mcc." + COLUMN_COMPARTMENTALIZED_COMPONENT_ID + " AND\n c." + COLUMN_BIGG_ID + " = ? AND\n m."
        + COLUMN_BIGG_ID + " = ? AND\n m." + COLUMN_ID + " = mcc." + COLUMN_MODEL_ID + " AND mcc.charge IS NOT NULL";
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, abbreviation);
      pStatement.setString(2, modelId);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        if (result > Integer.MIN_VALUE) {
          logger.severe("Charge query returned multiple results:\n" + query);
        } else {
        result = resultSet.getInt(1);}
      }
      pStatement.close();
      connection.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return result;
  }


  /**
   * @param reactionId
   * @return
   */
  public static boolean isPseudoreaction(String reactionId) {
    String query = "SELECT pseudoreaction FROM reaction WHERE bigg_id = ?";
    String result = singleParamStatement(query, reactionId.startsWith("R_") ? reactionId.substring(2) : reactionId);
    return (result != null) && result.equals("t");
  }


  /**
   * @param synonym
   * @param type
   * @param dataSourceId
   * @return String
   */
  public static String getBiggIdFromSynonym(String dataSourceId, String synonym, String type) {
    String biggId = null;
    String query;
    switch (type) {
    case TYPE_SPECIES:
      query = SELECT + "c." + COLUMN_BIGG_ID + FROM + COMPONENT + " c, " + DATA_SOURCE + " d, " + SYNONYM + " s" + WHERE
        + "d." + COLUMN_BIGG_ID + " = ? AND d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND s."
        + COLUMN_SYNONYM + " = ? AND s." + COLUMN_OME_ID + " = c." + COLUMN_ID;
      break;
    case TYPE_REACTION:
      query = SELECT + "r." + COLUMN_BIGG_ID + FROM + REACTION + " r, " + DATA_SOURCE + " d, " + SYNONYM + " s" + WHERE
        + "d." + COLUMN_BIGG_ID + " = ? AND d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND s."
        + COLUMN_SYNONYM + " = ? AND s." + COLUMN_OME_ID + " = r." + COLUMN_ID;
      break;
    case TYPE_GENE_PRODUCT:
      query = SELECT + "g." + COLUMN_LOCUS_TAG + FROM + GENE + " g, " + DATA_SOURCE + " d, " + SYNONYM + " s" + WHERE
        + "d." + COLUMN_BIGG_ID + " = ? AND d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND s."
        + COLUMN_SYNONYM + " = ? AND s." + COLUMN_OME_ID + " = g." + COLUMN_ID;
      break;
    default:
      return null;
    }
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, dataSourceId);
      pStatement.setString(2, synonym);
      ResultSet resultSet = pStatement.executeQuery();
      if (resultSet.next()) {
        biggId = resultSet.getString(1);
      } else {
        connection.close();
        return null;
      }
      // return null if more than one BiGG Id is obtained
      if (resultSet.next()) {
        connection.close();
        return null;
      }
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return biggId;
  }
}
