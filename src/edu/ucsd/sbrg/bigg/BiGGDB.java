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
package edu.ucsd.sbrg.bigg;

import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_BIGG_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_CHARGE;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_COMPARTMENTALIZED_COMPONENT_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_COMPARTMENT_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_COMPONENT_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_DATA_SOURCE_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_DATE_TIME;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_DESCRIPTION;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_FIRST_CREATED;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_FORMULA;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_GENE_REACTION_RULE;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_GENOME_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_MODEL_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_NAME;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_OME_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_ORGANISM;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_PSEUDOREACTION;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_PUBLICATION_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_REACTION_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_REFERENCE_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_REFERENCE_TYPE;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_SUBSYSTEM;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_TAXON_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COLUMN_TYPE;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COMPARTMENT;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COMPARTMENTALIZED_COMPONENT;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.COMPONENT;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.DATABASE_VERSION;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.DATA_SOURCE;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.GENOME;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.GENOME_REGION;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.MCC;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.MODEL;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.MODEL_REACTION;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.OLD_BIGG_ID;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.PUBLICATION;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.PUBLICATION_MODEL;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.REACTION;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.REFSEQ_NAME;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.REFSEQ_PATTERN;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.SYNONYM;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.URL;
import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants.URL_PREFIX;
import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;
import static org.sbml.jsbml.util.Pair.pairOf;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.sbml.jsbml.util.Pair;

import de.zbit.util.Utils;

/**
 * @author Andreas Dr&auml;ger
 */
public class BiGGDB {

  private static final String SELECT = "SELECT ";
  private static final String FROM = " FROM ";
  private static final String WHERE = " WHERE ";
  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(BiGGDB.class.getName());
  /**
   * The connection to the database.
   */
  private SQLConnector connector;


  /**
   * Initialize a SQL connection
   *
   * @param connector
   * @throws SQLException
   */
  BiGGDB(SQLConnector connector) throws SQLException {
    this.connector = connector;
    if (!connector.isConnected()) {
      connector.connect();
    }
  }


  /**
   *
   */
  void closeConnection() {
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
  public Date getBiGGVersion() {
    try {
      return getDate(SELECT + COLUMN_DATE_TIME + FROM + DATABASE_VERSION);
    } catch (SQLException exc) {
      logger.finest(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    return null;
  }


  // fixme: this currently only works for models which provide a model id that is present in bigg
  // subsystems for a reaction vary across models
  /**
   * @param modelBiGGid
   * @param reactionBiGGid
   * @return
   */
  public List<String> getSubsystems(String modelBiGGid, String reactionBiGGid) {
    String query = "SELECT DISTINCT mr." + COLUMN_SUBSYSTEM + "\n FROM " + REACTION + " r, " + MODEL + " m, "
      + MODEL_REACTION + " mr\n" + "WHERE m." + COLUMN_BIGG_ID + " = '%s' AND\n r." + COLUMN_BIGG_ID
      + " = '%s' AND\n m." + COLUMN_ID + " = mr." + COLUMN_MODEL_ID + " AND\n r." + COLUMN_ID + " = mr."
      + COLUMN_REACTION_ID + " AND\n LENGTH(mr." + COLUMN_SUBSYSTEM + ") > 0";
    List<String> list = new LinkedList<>();
    try {
      ResultSet rst = connector.query(query, modelBiGGid, reactionBiGGid);
      while (rst.next()) {
        list.add(rst.getString(1));
      }
      rst.getStatement().close();
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
  String getChemicalFormulaByCompartment(String componentId, String compartmentId) {
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
  String getChemicalFormula(String biggId, String modelId) {
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
  String getCompartmentName(BiGGId biggId) {
    return getString(SELECT + COLUMN_NAME + FROM + COMPARTMENT + WHERE + COLUMN_BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  String getComponentName(BiGGId biggId) throws SQLException {
    // unused
    return getString(SELECT + COLUMN_NAME + FROM + COMPONENT + WHERE + COLUMN_BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   */
  String getComponentType(BiGGId biggId) {
    return getString(SELECT + COLUMN_TYPE + FROM + COMPONENT + WHERE + COLUMN_BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param query
   * @return
   * @throws SQLException
   */
  private Date getDate(String query) throws SQLException {
    ResultSet rst = connector.query(query);
    Date result = rst.next() ? rst.getDate(1) : null;
    rst.getStatement().close();
    return result;
  }


  /**
   * Here we get all possible MIRIAM annotation for Gene Labels, but we ignore
   * all those entries that are not MIRIAM-compliant for now.
   *
   * @param label
   * @return
   */
  public TreeSet<String> getGeneIds(String label) {
    TreeSet<String> set = new TreeSet<>();
    String query = SELECT + URL_PREFIX + ", s." + SYNONYM + "\n" + "FROM  " + DATA_SOURCE + " d, " + SYNONYM + " s, "
      + GENOME_REGION + " gr\n" + "WHERE d." + COLUMN_ID + " = s." + COLUMN_DATA_SOURCE_ID + " AND\n s." + COLUMN_OME_ID
      + " = gr." + COLUMN_ID + " AND\n gr." + COLUMN_BIGG_ID + " = '%s' AND\n d." + COLUMN_BIGG_ID + " != "
      + OLD_BIGG_ID + " AND\n d." + COLUMN_BIGG_ID + " NOT LIKE " + REFSEQ_PATTERN;
    try {
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
        if ((resource = BiGGAnnotation.checkResourceUrl(resource)) != null) {
          set.add(resource);
        }
      }
      rst.getStatement().close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return set;
  }


  /**
   * @param label
   * @return
   */
  String getGeneName(String label) {
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
  public List<String> getGeneReactionRule(String reactionId, String modelId) {
    return getReactionRules("SELECT REPLACE(RTRIM(REPLACE(REPLACE(mr." + COLUMN_GENE_REACTION_RULE
      + ", 'or', '||'), 'and', '&&'), '.'), '.', '__SBML_DOT__') AS " + COLUMN_GENE_REACTION_RULE + FROM
      + MODEL_REACTION + " mr, " + REACTION + " r, " + MODEL + " m WHERE r." + COLUMN_ID + " = mr." + COLUMN_REACTION_ID
      + " AND m." + COLUMN_ID + " = mr." + COLUMN_MODEL_ID + " AND mr." + COLUMN_GENE_REACTION_RULE
      + " IS NOT NULL AND " + " LENGTH(mr." + COLUMN_GENE_REACTION_RULE + ") > 0 AND r." + COLUMN_BIGG_ID
      + " = '%s' AND m." + COLUMN_BIGG_ID + " = '%s' ORDER BY mr." + COLUMN_ID, reactionId, modelId);
  }


  /**
   * @param query
   * @param args
   * @return
   * @throws SQLException
   */
  private Integer getInt(String query, Object... args) throws SQLException {
    ResultSet rst = connector.query(query, args);
    Integer result = rst.next() ? rst.getInt(1) : null;
    rst.getStatement().close();
    return result;
  }


  /**
   * Do not use!
   *
   * @param biggId
   * @return
   * @throws SQLException
   */
  Date getModelCreationDate(BiGGId biggId) throws SQLException {
    // unused
    ResultSet rst = connector.query(SELECT + COLUMN_FIRST_CREATED + FROM + MODEL + WHERE + COLUMN_BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
    Date result = rst.next() ? rst.getDate(1) : null;
    rst.getStatement().close();
    return result;
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  String getModelDescription(String biggId) throws SQLException {
    // unsused
    return getString(SELECT + COLUMN_DESCRIPTION + FROM + MODEL + WHERE + COLUMN_BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @return
   */
  String getOrganism(String biggId) {
    return getString("SELECT g." + COLUMN_ORGANISM + FROM + GENOME + " g, " + MODEL + " m WHERE m." + COLUMN_GENOME_ID
      + " = g." + COLUMN_ID + " AND m." + COLUMN_BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public List<Pair<String, String>> getPublications(BiGGId biggId) throws SQLException {
    return getPublications(biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public List<Pair<String, String>> getPublications(String biggId) throws SQLException {
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
    return list;
  }


  /**
   * @param biggId
   * @return
   */
  public String getReactionName(String biggId) {
    return getString(SELECT + COLUMN_NAME + FROM + REACTION + WHERE + COLUMN_BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @param includeAnyURI
   * @param isReaction
   * @return a set of external source together with external id.
   * @throws SQLException
   */
  public TreeSet<String> getResources(BiGGId biggId, boolean includeAnyURI, boolean isReaction) throws SQLException {
    String type = isReaction ? REACTION : COMPONENT;
    ResultSet rst = connector.query(
      SELECT + connector.concat(URL_PREFIX, "s." + SYNONYM) + " AS " + URL + FROM + type + " t, " + SYNONYM + " s, "
        + DATA_SOURCE + " d WHERE t." + COLUMN_ID + " = s." + COLUMN_OME_ID + " AND s." + COLUMN_DATA_SOURCE_ID
        + " = d." + COLUMN_ID + " AND " + URL_PREFIX + " IS NOT NULL AND " + getTypeQuery(isReaction) + " AND t."
        + COLUMN_BIGG_ID + " = '%s'%s",
      biggId.getAbbreviation(), includeAnyURI ? "" : " AND " + URL_PREFIX + " like '%%identifiers.org%%'");
    TreeSet<String> result = new TreeSet<>();
    while (rst.next()) {
      String resource = rst.getString(1);
      resource = BiGGAnnotation.checkResourceUrl(resource);
      if (resource != null) {
        result.add(resource);
      }
    }
    rst.getStatement().close();
    return result;
  }


  /**
   * @param isReaction
   * @return
   */
  private String getTypeQuery(boolean isReaction) {
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
  public List<String> getReactionRules(String query, Object... args) {
    try {
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
  public String getString(String query, Object... args) {
    try {
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
      // we don't need to do anything here
      rst.getStatement().close();
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
  public String getStringUnique(String query, String componentID, String compartmentID) {
    String type = query.contains("charge") ? "charge" : "formula";
    try {
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
  public Integer getTaxonId(String biggId) {
    try {
      return getInt(SELECT + COLUMN_TAXON_ID + FROM + GENOME + " g, " + MODEL + " m WHERE g." + COLUMN_ID + " = m."
        + COLUMN_GENOME_ID + " AND m." + COLUMN_BIGG_ID + " = '%s'", biggId);
    } catch (SQLException exc) {
      logger.warning(format(mpMessageBundle.getString("GET_TAXON_ERROR"), biggId, Utils.getMessage(exc)));
    }
    return null;
  }


  /**
   * @param biggId
   * @return
   */
  public boolean isCompartment(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM " + COMPARTMENT + WHERE + COLUMN_BIGG_ID + " = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(format(mpMessageBundle.getString("IS_COMPARTMENT_FAILED"), biggId, Utils.getMessage(exc)));
    }
    return false;
  }


  /**
   * @param biggId
   * @return
   */
  public boolean isMetabolite(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM " + COMPONENT + WHERE + COLUMN_BIGG_ID + " = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(format(mpMessageBundle.getString("IS_METABOLITE_FAILED"), biggId, Utils.getMessage(exc)));
    }
    return false;
  }


  /**
   * @param biggId
   * @return
   */
  public boolean isModel(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM " + MODEL + WHERE + COLUMN_BIGG_ID + " = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(format(mpMessageBundle.getString("IS_MODEL_FAILED"), biggId, Utils.getMessage(exc)));
    }
    return false;
  }


  /**
   * @param biggId
   * @return
   */
  public boolean isReaction(String biggId) {
    if (biggId.startsWith("R_")) {
      biggId = biggId.substring(2);
    }
    try {
      return getInt("SELECT COUNT(*) FROM " + REACTION + WHERE + COLUMN_BIGG_ID + " = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(format(mpMessageBundle.getString("IS_REACTION_FAILED"), biggId, Utils.getMessage(exc)));
    }
    return false;
  }


  /**
   * Get charge for unknown model id
   *
   * @param componentId
   * @param compartmentId
   * @return
   */
  Integer getChargeByCompartment(String componentId, String compartmentId) {
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
  public Integer getCharge(String biggId, String modelId) {
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
  public boolean isPseudoreaction(String reactionId) {
    String query = SELECT + COLUMN_PSEUDOREACTION + FROM + REACTION + WHERE + COLUMN_BIGG_ID + " = '%s'";
    String result = getString(query, reactionId.startsWith("R_") ? reactionId.substring(2) : reactionId);
    return (result != null) && result.equals("t");
  }
}
