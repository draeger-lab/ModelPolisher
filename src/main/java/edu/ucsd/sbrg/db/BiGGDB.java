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
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_BIGG_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_CHARGE;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_COMPARTMENTALIZED_COMPONENT_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_COMPARTMENT_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_COMPONENT_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_DATA_SOURCE_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_DATE_TIME;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_FORMULA;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_GENE_REACTION_RULE;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_GENOME_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_LOCUS_TAG;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_MODEL_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_NAME;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_OME_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_ORGANISM;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_PSEUDOREACTION;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_PUBLICATION_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_REACTION_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_REFERENCE_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_REFERENCE_TYPE;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_SYNONYM;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_TAXON_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COLUMN_TYPE;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COMPARTMENT;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COMPARTMENTALIZED_COMPONENT;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.COMPONENT;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.DATABASE_VERSION;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.DATA_SOURCE;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.GENE;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.GENOME;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.GENOME_REGION;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.MCC;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.MODEL;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.MODEL_REACTION;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.OLD_BIGG_ID;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.PUBLICATION;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.PUBLICATION_MODEL;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.REACTION;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.REFSEQ_NAME;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.REFSEQ_PATTERN;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.SYNONYM;
import static edu.ucsd.sbrg.db.BiGGDBContract.Constants.URL_PREFIX;
import static java.text.MessageFormat.format;
import static org.sbml.jsbml.util.Pair.pairOf;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.sbml.jsbml.ext.fbc.GeneProduct;
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
  private static SQLConnector connector;

  /**
   * Don't allow instantiation
   */
  private BiGGDB() {
  };


  public static void init(String host, String port, String user, String passwd, String name)
    throws ClassNotFoundException {
    connector = new PostgreSQLConnector(host, Integer.parseInt(port), user, passwd != null ? passwd : "", name);
  }


  /**
   *
   */
  private static void startConnection() throws SQLException {
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
   * @return
   */
  public static Date getBiGGVersion() {
    Date date = null;
    try {
      startConnection();
      date = getDate(SELECT + COLUMN_DATE_TIME + FROM + DATABASE_VERSION);
      closeConnection();
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
    // fixme: this currently only works for models which provide a model id that is present in bigg
    // subsystems for a reaction vary across models
    String query = "SELECT DISTINCT mr.subsystem FROM reaction r, model m, model_reaction mr WHERE m.bigg_id = ? "
      + "AND r.bigg_id = ? AND m.id = mr.model_id AND r.id = mr.reaction_id AND LENGTH(mr.subsystem) > 0";
    List<String> list = new LinkedList<>();
    try {
      startConnection();
      PreparedStatement pStatement = connector.getConnection().prepareStatement(query);
      pStatement.setString(1, modelBiGGid);
      pStatement.setString(2, reactionBiGGid);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        list.add(resultSet.getString(1));
      }
      resultSet.getStatement().close();
      closeConnection();
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
    String query = "SELECT DISTINCT mcc." + COLUMN_FORMULA + " FROM " + MCC + " mcc, " + COMPARTMENTALIZED_COMPONENT
      + " cc, " + COMPONENT + " c, " + COMPARTMENT + " co WHERE c." + COLUMN_BIGG_ID + " = '%s' AND c." + COLUMN_ID
      + " = cc." + COLUMN_COMPONENT_ID + " AND co." + COLUMN_BIGG_ID + " = '%s' AND co." + COLUMN_ID + " = cc."
      + COLUMN_COMPARTMENT_ID + " and cc." + COLUMN_ID + " = mcc." + COLUMN_COMPARTMENTALIZED_COMPONENT_ID
      + " AND LENGTH(mcc." + COLUMN_FORMULA + ") > 0 ORDER BY mcc." + COLUMN_FORMULA;
    return getStringUnique(query, componentId, compartmentId);
  }


  /**
   * Get chemical formula for models that are present in BiGG
   *
   * @param biggId
   * @param modelId
   * @return
   */
  public static String getChemicalFormula(String biggId, String modelId) {
    String query =
      "SELECT DISTINCT mcc." + COLUMN_FORMULA + "\n FROM " + COMPONENT + " c,\n" + COMPARTMENTALIZED_COMPONENT
        + " cc,\n" + MODEL + " m,\n" + MCC + " mcc\n WHERE c." + COLUMN_ID + " = cc." + COLUMN_COMPONENT_ID
        + " AND\n cc." + COLUMN_ID + " = mcc." + COLUMN_COMPARTMENTALIZED_COMPONENT_ID + " AND\n c." + COLUMN_BIGG_ID
        + " = '%s' AND\n m." + COLUMN_BIGG_ID + " = '%s' AND\n m." + COLUMN_ID + " = mcc." + COLUMN_MODEL_ID;
    return getString(query, biggId, modelId);
  }


  /**
   * @param biggId
   * @return
   */
  public static String getCompartmentName(BiGGId biggId) {
    return getString(SELECT + COLUMN_NAME + FROM + COMPARTMENT + WHERE + COLUMN_BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public static String getComponentName(BiGGId biggId) throws SQLException {
    // unused
    return getString(SELECT + COLUMN_NAME + FROM + COMPONENT + WHERE + COLUMN_BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   */
  public static String getComponentType(BiGGId biggId) {
    return getString(SELECT + COLUMN_TYPE + FROM + COMPONENT + WHERE + COLUMN_BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param query
   * @return
   * @throws SQLException
   */
  private static Date getDate(String query) throws SQLException {
    startConnection();
    ResultSet rst = connector.query(query);
    Date result = rst.next() ? rst.getDate(1) : null;
    rst.getStatement().close();
    closeConnection();
    return result;
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
      + " = gr." + COLUMN_ID + " AND\n gr." + COLUMN_BIGG_ID + " = '%s' AND\n d." + COLUMN_BIGG_ID + " != "
      + OLD_BIGG_ID + " AND\n d." + COLUMN_BIGG_ID + " NOT LIKE " + REFSEQ_PATTERN;
    try {
      startConnection();
      ResultSet rst = connector.query(query, label);
      while (rst.next()) {
        String resource;
        String collection = rst.getString(1);
        String identifier = rst.getString(2);
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
      rst.getStatement().close();
      closeConnection();
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
      + COLUMN_ID + " AND\n gr." + COLUMN_BIGG_ID + " = '%s' AND\n d." + COLUMN_BIGG_ID + " LIKE " + REFSEQ_NAME;
    return getString(query, label);
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
      + " IS NOT NULL AND  LENGTH(mr." + COLUMN_GENE_REACTION_RULE + ") > 0 AND r." + COLUMN_BIGG_ID + " = '%s' AND m."
      + COLUMN_BIGG_ID + " = '%s' ORDER BY mr." + COLUMN_ID, reactionId, modelId);
  }


  /**
   * @param query
   * @param args
   * @return
   * @throws SQLException
   */
  private static Integer getInt(String query, Object... args) throws SQLException {
    startConnection();
    ResultSet rst = connector.query(query, args);
    Integer result = rst.next() ? rst.getInt(1) : null;
    rst.getStatement().close();
    closeConnection();
    return result;
  }


  /**
   * @param biggId
   * @return
   */
  public static String getOrganism(String biggId) {
    return getString("SELECT g." + COLUMN_ORGANISM + FROM + GENOME + " g, " + MODEL + " m WHERE m." + COLUMN_GENOME_ID
      + " = g." + COLUMN_ID + " AND m." + COLUMN_BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public static List<Pair<String, String>> getPublications(String biggId) throws SQLException {
    startConnection();
    ResultSet rst =
      connector.query("SELECT p." + COLUMN_REFERENCE_TYPE + ", p." + COLUMN_REFERENCE_ID + " FROM  " + PUBLICATION
        + " p, " + PUBLICATION_MODEL + " pm, " + MODEL + " m WHERE p." + COLUMN_ID + " = pm." + COLUMN_PUBLICATION_ID
        + " AND pm." + COLUMN_MODEL_ID + " = m." + COLUMN_ID + " AND m." + COLUMN_BIGG_ID + " = '%s'", biggId);
    List<Pair<String, String>> list = new LinkedList<>();
    while (rst.next()) {
      String key = rst.getString(1);
      list.add(pairOf(key.equals("pmid") ? "pubmed" : key, rst.getString(2)));
    }
    rst.getStatement().close();
    closeConnection();
    return list;
  }


  /**
   * @param biggId
   * @return
   */
  public static String getReactionName(String biggId) {
    return getString(SELECT + COLUMN_NAME + FROM + REACTION + WHERE + COLUMN_BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @param includeAnyURI
   * @param isReaction
   * @return a set of external source together with external id.
   * @throws SQLException
   */
  public static TreeSet<String> getResources(BiGGId biggId, boolean includeAnyURI, boolean isReaction)
    throws SQLException {
    String type = isReaction ? REACTION : COMPONENT;
    startConnection();
    String query = String.format(
      "SELECT %s AS url FROM %s t, synonym s, data_source d WHERE t.id = s.ome_id AND s.data_source_id = d.id AND url_prefix IS NOT NULL AND %s AND t.bigg_id = ?%s",
      connector.concat(URL_PREFIX, "s." + SYNONYM), type, getTypeQuery(isReaction),
      includeAnyURI ? "" : " AND url_prefix like '%%identifiers.org%%'");
    PreparedStatement pStatement = connector.getConnection().prepareStatement(query);
    pStatement.setString(1, biggId.getAbbreviation());
    ;
    ResultSet resultSet = pStatement.executeQuery();
    TreeSet<String> resources = new TreeSet<>();
    while (resultSet.next()) {
      String resource = resultSet.getString(1);
      resource = Registry.checkResourceUrl(resource);
      if (resource != null) {
        resources.add(resource);
      }
    }
    resultSet.close();
    pStatement.close();
    closeConnection();
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
   * @param query
   * @param args
   * @return
   */
  public static List<String> getReactionRules(String query, Object... args) {
    try {
      startConnection();
      ResultSet rst = connector.query(query, args);
      List<String> result = new ArrayList<>();
      if (rst.isBeforeFirst()) {
        while (rst.next()) {
          String tmp = rst.getString(1);
          if (tmp != null) {
            result.add(tmp);
          }
        }
      }
      rst.getStatement().close();
      closeConnection();
      return result;
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return new ArrayList<>();
  }


  /**
   * @param query
   * @param args
   * @return
   */
  public static String getString(String query, Object... args) {
    try {
      startConnection();
      ResultSet rst = connector.query(query, args);
      String result = "";
      if (rst.isBeforeFirst()) {
        while (rst.next()) {
          result = rst.getString(1);
          if (result != null && !result.trim().isEmpty()) {
            break;
          }
        }
      }
      // Only a result set from getCharge() can have multiple entries, as it is corrected later on,
      // we don't need to do anything here -- What does this mean?
      rst.getStatement().close();
      closeConnection();
      return result;
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return "";
  }


  /**
   * @param query
   * @param componentID
   * @param compartmentID
   * @return
   */
  public static String getStringUnique(String query, String componentID, String compartmentID) {
    String type = query.contains("charge") ? "charge" : "formula";
    try {
      startConnection();
      ResultSet rst = connector.query(query, componentID, compartmentID);
      String result = "";
      if (rst.isBeforeFirst()) {
        while (rst.next()) {
          result = rst.getString(1);
          if (result != null && !result.trim().isEmpty()) {
            break;
          }
        }
      }
      if (rst.next()) {
        String tmp = rst.getString(1);
        if (tmp != null && !tmp.trim().isEmpty()) {
          logger.info(format(mpMessageBundle.getString("RST_NOT_UNIQUE"), type, componentID, compartmentID));
          return "";
        }
      }
      rst.getStatement().close();
      closeConnection();
      return result;
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return "";
  }


  /**
   * @param biggId
   * @return
   */
  public static Integer getTaxonId(String biggId) {
    try {
      return getInt(SELECT + COLUMN_TAXON_ID + FROM + GENOME + " g, " + MODEL + " m WHERE g." + COLUMN_ID + " = m."
        + COLUMN_GENOME_ID + " AND m." + COLUMN_BIGG_ID + " = '%s'", biggId);
    } catch (SQLException exc) {
      logger.warning(format(mpMessageBundle.getString("GET_TAXON_ERROR"), biggId, Utils.getMessage(exc)));
    }
    return null;
  }


  public static Set<String> getOnce(String table) {
    Set<String> biggIds = new LinkedHashSet<>();
    try {
      startConnection();
      String query = "SELECT bigg_id FROM " + table + " ORDER BY bigg_id";
      PreparedStatement pStatement = connector.getConnection().prepareStatement(query);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        biggIds.add(resultSet.getString("bigg_id"));
      }
      resultSet.close();
      pStatement.close();
      closeConnection();
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
  public static Integer getChargeByCompartment(String componentId, String compartmentId) {
    String query = "SELECT DISTINCT mcc." + COLUMN_CHARGE + " FROM " + MCC + " mcc, " + COMPARTMENTALIZED_COMPONENT
      + " cc, " + COMPONENT + " c, " + COMPARTMENT + " co WHERE c." + COLUMN_BIGG_ID + " = '%s' AND c." + COLUMN_ID
      + " = cc." + COLUMN_COMPONENT_ID + " AND co." + COLUMN_BIGG_ID + " = '%s' AND co." + COLUMN_ID + " = cc."
      + COLUMN_COMPARTMENT_ID + " and cc." + COLUMN_ID + " = mcc." + COLUMN_COMPARTMENTALIZED_COMPONENT_ID
      + " AND LENGTH(CAST( mcc." + COLUMN_CHARGE + " AS text)) > 0 ORDER BY mcc." + COLUMN_CHARGE;
    String charge = getStringUnique(query, componentId, compartmentId);
    if (charge == null || charge.trim().length() == 0) {
      return null;
    }
    return Integer.parseInt(charge);
  }


  /**
   * Get charge for known model id
   *
   * @param biggId
   * @param modelId
   * @return
   */
  public static Integer getCharge(String biggId, String modelId) {
    String query =
      "SELECT DISTINCT mcc." + COLUMN_CHARGE + "\n FROM " + COMPONENT + " c,\n" + COMPARTMENTALIZED_COMPONENT + " cc,\n"
        + MODEL + " m,\n" + MCC + " mcc\n WHERE c." + COLUMN_ID + " = cc." + COLUMN_COMPONENT_ID + " AND\n cc."
        + COLUMN_ID + " = mcc." + COLUMN_COMPARTMENTALIZED_COMPONENT_ID + " AND\n c." + COLUMN_BIGG_ID
        + " = '%s' AND\n m." + COLUMN_BIGG_ID + " = '%s' AND\n m." + COLUMN_ID + " = mcc." + COLUMN_MODEL_ID;
    String charge = getString(query, biggId, modelId);
    if ((charge == null) || (charge.trim().length() == 0)) {
      return null;
    }
    return Integer.parseInt(charge);
  }


  /**
   * @param reactionId
   * @return
   */
  public static boolean isPseudoreaction(String reactionId) {
    String query = SELECT + COLUMN_PSEUDOREACTION + FROM + REACTION + WHERE + COLUMN_BIGG_ID + " = '%s'";
    String result = getString(query, reactionId.startsWith("R_") ? reactionId.substring(2) : reactionId);
    return (result != null) && result.equals("t");
  }


  /**
   * @param synonym
   * @param type
   * @param data_source_biggId
   * @return String
   */
  public static String getBiggIdFromSynonym(String data_source_biggId, String synonym, String type) {
    String biggId = "";
    String query;
    switch (type) {
    case TYPE_SPECIES:
      query = SELECT + "c." + COLUMN_BIGG_ID + FROM + COMPONENT + " c, " + DATA_SOURCE + " d, " + SYNONYM + " s" + WHERE
        + "d." + COLUMN_BIGG_ID + " = '%s' AND d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND s."
        + COLUMN_SYNONYM + " = '%s' AND s." + COLUMN_OME_ID + " = c." + COLUMN_ID;
      break;
    case TYPE_REACTION:
      query = SELECT + "r." + COLUMN_BIGG_ID + FROM + REACTION + " r, " + DATA_SOURCE + " d, " + SYNONYM + " s" + WHERE
        + "d." + COLUMN_BIGG_ID + " = '%s' AND d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND s."
        + COLUMN_SYNONYM + " = '%s' AND s." + COLUMN_OME_ID + " = r." + COLUMN_ID;
      break;
    case TYPE_GENE_PRODUCT:
      query = SELECT + "g." + COLUMN_LOCUS_TAG + FROM + GENE + " g, " + DATA_SOURCE + " d, " + SYNONYM + " s" + WHERE
        + "d." + COLUMN_BIGG_ID + " = '%s' AND d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND s."
        + COLUMN_SYNONYM + " = '%s' AND s." + COLUMN_OME_ID + " = g." + COLUMN_ID;
      break;
    default:
      return null;
    }
    try {
      startConnection();
      ResultSet rst = connector.query(query, data_source_biggId, synonym);
      if (rst.next()) {
        biggId = rst.getString(1);
      } else {
        closeConnection();
        return null;
      }
      // return null if more than one BiGG Id is obtained
      if (rst.next()) {
        closeConnection();
        return null;
      }
      closeConnection();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return biggId;
  }


  /**
   * @param geneProduct
   * @return boolean
   */
  public static boolean isGenePresentInBigg(GeneProduct geneProduct) {
    String id = geneProduct.getId();
    if (id.startsWith("G_")) {
      id = id.substring(2);
    }
    String query = SELECT + COLUMN_LOCUS_TAG + FROM + GENE + WHERE + COLUMN_LOCUS_TAG + " = '%s'";
    try {
      startConnection();
      ResultSet rst = connector.query(query, id);
      closeConnection();
      return rst.next();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return false;
  }
}
