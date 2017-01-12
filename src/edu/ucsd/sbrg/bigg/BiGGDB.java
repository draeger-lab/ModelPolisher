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

import de.zbit.util.Utils;
import org.sbml.jsbml.util.Pair;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static edu.ucsd.sbrg.bigg.BiGGDBContract.Constants;
import static org.sbml.jsbml.util.Pair.pairOf;

/**
 * @author Andreas Dr&auml;ger
 */
public class BiGGDB {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger =
    Logger.getLogger(BiGGDB.class.getName());
  /**
   * The connection to the database.
   */
  private PostgreSQLConnector connect;


  /**
   * @param connector
   * @throws SQLException
   */
  public BiGGDB(PostgreSQLConnector connector) throws SQLException {
    connect = connector;
    if (!connector.isConnected()) {
      connector.connect();
    }
  }


  /**
   * @return
   */
  public Date getBiGGVersion() {
    try {
      return getDate("SELECT " + Constants.DATE_TIME + " FROM "
        + Constants.DATABASE_VERSION);
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format("{0}: {1}", exc.getClass().getName(),
        Utils.getMessage(exc)));
    }
    return null;
  }


  /**
   * @param modelBiGGid
   * @param reactionBiGGid
   * @return
   */
  public List<String> getSubsystems(String modelBiGGid, String reactionBiGGid) {
    String query = "SELECT DISTINCT mr." + Constants.SUBSYSTEM + "\n FROM "
      + Constants.REACTION + " r, " + Constants.MODEL + " m, "
      + Constants.MODEL_REACTION + " mr\n" + "WHERE m." + Constants.BIGG_ID
      + " = '%s' AND\n r." + Constants.BIGG_ID + " = '%s' AND\n m."
      + Constants.ID + " = mr." + Constants.MODEL_ID + " AND\n r."
      + Constants.ID + " = mr." + Constants.REACTION_ID + " AND\n length(mr."
      + Constants.SUBSYSTEM + ") > 0";
    List<String> list = new LinkedList<String>();
    try {
      ResultSet rst = connect.query(query, modelBiGGid, reactionBiGGid);
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
   * @param biggId
   * @return
   */
  public String getChemicalFormula(BiGGId biggId, String modelId) {
    String query = "SELECT mcc." + Constants.FORMULA + "\n FROM "
      + Constants.COMPONENT + " c,\n" + Constants.COMPARTMENTALIZED_COMPONENT
      + " cc,\n" + Constants.MODEL + " m,\n" + Constants.MCC + " mcc\n WHERE c."
      + Constants.ID + " = cc." + Constants.COMPONENT_ID + " AND\n cc."
      + Constants.ID + " = mcc." + Constants.COMPARTMENTALIZED_COMPONENT_ID
      + " AND\n c." + Constants.BIGG_ID + " = '%s' AND\n m." + Constants.BIGG_ID
      + " = '%s' AND\n m." + Constants.ID + " = mcc." + Constants.MODEL_ID
      + ";";
    return getString(query, biggId.getAbbreviation(), modelId);
  }


  /**
   * @param biggId
   * @return
   */
  public String getCompartmentName(BiGGId biggId) {
    return getString("SELECT " + Constants.NAME + " FROM "
      + Constants.COMPARTMENT + " WHERE " + Constants.BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public String getComponentName(BiGGId biggId) throws SQLException {
    return getString("SELECT " + Constants.NAME + " FROM " + Constants.COMPONENT
      + " WHERE " + Constants.BIGG_ID + " = '%s'", biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @param includeAnyURI
   * @return a list of external source together with external id.
   * @throws SQLException
   */
  public List<String> getComponentResources(BiGGId biggId,
    boolean includeAnyURI) throws SQLException {
    return getResourceURL(biggId, Constants.COMPONENT, includeAnyURI);
  }


  /**
   * @param biggId
   * @return
   */
  public String getComponentType(BiGGId biggId) {
    return getString("SELECT " + Constants.TYPE + " FROM " + Constants.COMPONENT
      + " WHERE " + Constants.BIGG_ID + " = '%s'", biggId.getAbbreviation());
  }


  /**
   * @param query
   * @return
   * @throws SQLException
   */
  private Date getDate(String query) throws SQLException {
    ResultSet rst = connect.query(query);
    Date result = rst.next() ? result = rst.getDate(1) : null;
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
  public List<Pair<String, String>> getGeneIds(String label) {
    List<Pair<String, String>> list = new LinkedList<Pair<String, String>>();
    String query = "SELECT d." + Constants.BIGG_ID + ", s." + Constants.SYNONYM
      + "\n" + "FROM  " + Constants.DATA_SOURCE + " d, " + Constants.SYNONYM
      + " s, " + Constants.GENOME_REGION + " gr\n" + "WHERE d." + Constants.ID
      + " = s." + Constants.DATA_SOURCE_ID + " AND\n s." + Constants.OME_ID
      + " = gr." + Constants.ID + " AND\n gr." + Constants.BIGG_ID
      + " = '%s' AND\n d." + Constants.BIGG_ID + " != " + Constants.OLD_BIGG_ID
      + " AND\n d." + Constants.BIGG_ID + " NOT LIKE "
      + Constants.REFSEQ_PATTERN;
    try {
      ResultSet rst = connect.query(query, label);
      while (rst.next()) {
        list.add(pairOf(rst.getString(1), rst.getString(2)));
      }
      rst.getStatement().close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return list;
  }


  /**
   * @param label
   * @return
   */
  public String getGeneName(String label) {
    String query = "SELECT s." + Constants.SYNONYM + "\n" + "FROM  "
      + Constants.DATA_SOURCE + " d, " + Constants.SYNONYM + " s, "
      + Constants.GENOME_REGION + " gr\n" + "WHERE d." + Constants.ID + " = s."
      + Constants.DATA_SOURCE_ID + " AND\n s." + Constants.OME_ID + " = gr."
      + Constants.ID + " AND\n gr." + Constants.BIGG_ID + " = '%s' AND\n d."
      + Constants.BIGG_ID + " = " + Constants.REFSEQ_NAME;
    return getString(query, label);
  }


  /**
   * @param reactionId
   * @param modelId
   * @return
   */
  public String getGeneReactionRule(String reactionId, String modelId) {
    return getString(
      "SELECT REPLACE(RTRIM(REPLACE(REPLACE(mr." + Constants.GENE_REACTION_RULE
        + ", 'or', '||'), 'and', '&&'), '.'), '.', '__SBML_DOT__') AS "
        + Constants.GENE_REACTION_RULE + " FROM " + Constants.MODEL_REACTION
        + " mr, " + Constants.REACTION + " r, " + Constants.MODEL
        + " m WHERE r." + Constants.ID + " = mr." + Constants.REACTION_ID
        + " AND m." + Constants.ID + " = mr." + Constants.MODEL_ID + " AND mr."
        + Constants.GENE_REACTION_RULE + " IS NOT NULL AND " + " LENGTH(mr."
        + Constants.GENE_REACTION_RULE + ") > 0 AND r." + Constants.BIGG_ID
        + " = '%s' AND m." + Constants.BIGG_ID + " = '%s'",
      reactionId, modelId);
  }


  /**
   * @param query
   * @param args
   * @return
   * @throws SQLException
   */
  private Integer getInt(String query, Object... args) throws SQLException {
    ResultSet rst = connect.query(query, args);
    Integer result = rst.next() ? result = rst.getInt(1) : null;
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
  public Date getModelCreationDate(BiGGId biggId) throws SQLException {
    ResultSet rst =
      connect.query(
        "SELECT " + Constants.FIRST_CREATED + " FROM " + Constants.MODEL
          + " WHERE " + Constants.BIGG_ID + " = '%s'",
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
  public String getModelDescription(String biggId) throws SQLException {
    return getString("SELECT " + Constants.DESCRIPTION + " FROM "
      + Constants.MODEL + " WHERE " + Constants.BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @return
   */
  public String getOrganism(String biggId) {
    return getString(
      "SELECT g." + Constants.ORGANISM + " FROM " + Constants.GENOME + " g, "
        + Constants.MODEL + " m WHERE m." + Constants.GENOME_ID + " = g."
        + Constants.ID + " AND m." + Constants.BIGG_ID + " = '%s'",
      biggId);
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public List<Pair<String, String>> getPublications(BiGGId biggId)
    throws SQLException {
    return getPublications(biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public List<Pair<String, String>> getPublications(String biggId)
    throws SQLException {
    ResultSet rst = connect.query("SELECT p." + Constants.REFERENCE_TYPE
      + ", p." + Constants.REFERENCE_ID + " FROM  " + Constants.PUBLICATION
      + " p, " + Constants.PUBLICATION_MODEL + " pm, " + Constants.MODEL
      + " m WHERE p." + Constants.ID + " = pm." + Constants.PUBLICATION_ID
      + " AND pm." + Constants.MODEL_ID + " = m." + Constants.ID + " AND m."
      + Constants.BIGG_ID + " = '%s'", biggId);
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
    return getString("SELECT " + Constants.NAME + " FROM " + Constants.REACTION
      + " WHERE " + Constants.BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @param type
   * @param includeAnyURI
   * @return a list of external source together with external id.
   * @throws SQLException
   */
  private List<String> getResourceURL(BiGGId biggId, String type,
    boolean includeAnyURI) throws SQLException {
    // TODO: there could be errors in BiGG database and it is important to check
    // patterns again (see MIRIAM class).
    ResultSet rst = connect.query(
      "SELECT CONCAT(" + Constants.URL_PREFIX + ", s." + Constants.SYNONYM
        + ") AS " + Constants.URL + " FROM  %s c, " + Constants.SYNONYM + " s, "
        + Constants.DATA_SOURCE + " d WHERE c." + Constants.ID + " = s."
        + Constants.OME_ID + " AND s." + Constants.DATA_SOURCE_ID + " = d."
        + Constants.ID + " AND " + Constants.URL_PREFIX + " IS NOT NULL AND c."
        + Constants.BIGG_ID + " = '%s'%s",
      type, biggId.getAbbreviation(), includeAnyURI ? ""
        : " AND " + Constants.URL_PREFIX + " like '%%identifiers.org%%'");
    List<String> result = new LinkedList<String>();
    while (rst.next()) {
      result.add(rst.getString(1));
    }
    rst.getStatement().close();
    return result;
  }


  /**
   * @param query
   * @param args
   * @return
   */
  public String getString(String query, Object... args) {
    try {
      ResultSet rst = connect.query(query, args);
      String result = rst.next() ? rst.getString(1) : "";
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
      return getInt(
        "SELECT " + Constants.TAXON_ID + " FROM " + Constants.GENOME + " g, "
          + Constants.MODEL + " m WHERE g." + Constants.ID + " = m."
          + Constants.GENOME_ID + " AND m." + Constants.BIGG_ID + " = '%s'",
        biggId);
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not retrieve NCBI taxon identifier for model ''{0}'', because of {1}.",
        biggId, Utils.getMessage(exc)));
    }
    return null;
  }


  /**
   * @param biggId
   * @return
   */
  public boolean isCompartment(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM " + Constants.COMPARTMENT + " WHERE "
        + Constants.BIGG_ID + " = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not determine if ''{0}'' is a compartment or not: {1}.", biggId,
        Utils.getMessage(exc)));
    }
    return false;
  }


  /**
   * @param biggId
   * @return
   */
  public boolean isMetabolite(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM " + Constants.COMPONENT + " WHERE "
        + Constants.BIGG_ID + " = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not determine if ''{0}'' is a metabolite or not: {1}.", biggId,
        Utils.getMessage(exc)));
    }
    return false;
  }


  /**
   * @param biggId
   * @return
   */
  public boolean isModel(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM " + Constants.MODEL + " WHERE "
        + Constants.BIGG_ID + " = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not determine if ''{0}'' is a model or not: {1}.", biggId,
        Utils.getMessage(exc)));
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
      return getInt("SELECT COUNT(*) FROM " + Constants.REACTION + " WHERE "
        + Constants.BIGG_ID + " = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not determine if ''{0}'' is a reaction or not: {1}.", biggId,
        Utils.getMessage(exc)));
    }
    return false;
  }


  /**
   * @param biggId
   * @param modelId
   * @return
   */
  public Integer getCharge(String biggId, String modelId) {
    String query = "SELECT mcc." + Constants.CHARGE + "\n FROM "
      + Constants.COMPONENT + " c,\n" + Constants.COMPARTMENTALIZED_COMPONENT
      + " cc,\n" + Constants.MODEL + " m,\n" + Constants.MCC + " mcc\n WHERE c."
      + Constants.ID + " = cc." + Constants.COMPONENT_ID + " AND\n cc."
      + Constants.ID + " = mcc." + Constants.COMPARTMENTALIZED_COMPONENT_ID
      + " AND\n c." + Constants.BIGG_ID + " = '%s' AND\n m." + Constants.BIGG_ID
      + " = '%s' AND\n m." + Constants.ID + " = mcc." + Constants.MODEL_ID;
    String charge = getString(query, biggId, modelId);
    if ((charge == null) || (charge.trim().length() == 0)) {
      return null;
    }
    return charge != null ? Integer.valueOf(Integer.parseInt(charge)) : null;
  }


  /**
   * @param reactionId
   * @return
   */
  public boolean isPseudoreaction(String reactionId) {
    String query = "SELECT " + Constants.PSEUDOREACTION + " FROM "
      + Constants.REACTION + " WHERE " + Constants.BIGG_ID + " = '%s'";
    String result = getString(query,
      reactionId.startsWith("R_") ? reactionId.substring(2) : reactionId);
    return (result != null) && result.equals("t");
  }
}
