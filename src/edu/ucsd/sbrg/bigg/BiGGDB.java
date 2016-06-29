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

import static org.sbml.jsbml.util.Pair.pairOf;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.sbml.jsbml.util.Pair;

import de.zbit.util.Utils;

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
      return getDate("SELECT " + BiGGDBContract.BiGGDBConstants.DATE_TIME
        + " FROM " + BiGGDBContract.BiGGDBConstants.DATABASE_VERSION);
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
    String query = "SELECT DISTINCT "
      + BiGGDBContract.BiGGDBConstants.MR_SUBSYSTEM + "\n" + "FROM "
      + BiGGDBContract.BiGGDBConstants.REACTION + ", "
      + BiGGDBContract.BiGGDBConstants.MODEL + ", "
      + BiGGDBContract.BiGGDBConstants.MODEl_REACTION + "\n" + "WHERE "
      + BiGGDBContract.BiGGDBConstants.M_BIGG_ID + " = '%s' AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.R_BIGG_ID + " = '%s' AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.M_ID + " = "
      + BiGGDBContract.BiGGDBConstants.MR_MODEL_ID + " AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.R_ID + " = "
      + BiGGDBContract.BiGGDBConstants.MR_REACTION_ID + " AND\n"
      + "      length(" + BiGGDBContract.BiGGDBConstants.MR_SUBSYSTEM + ") > 0";
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
    String query = "SELECT " + BiGGDBContract.BiGGDBConstants.MCC_FORMULA + "\n"
      + "FROM   " + BiGGDBContract.BiGGDBConstants.COMPONENT + ",\n" + "       "
      + BiGGDBContract.BiGGDBConstants.COMPARTMENTALIZED_COMPONENT + ",\n"
      + "       " + BiGGDBContract.BiGGDBConstants.MODEL + ",\n" + "       "
      + BiGGDBContract.BiGGDBConstants.MCC + "\n" + "WHERE  "
      + BiGGDBContract.BiGGDBConstants.C_ID + " = "
      + BiGGDBContract.BiGGDBConstants.CC_COMPONENT_ID + " AND\n" + "       "
      + BiGGDBContract.BiGGDBConstants.CC_ID + " = "
      + BiGGDBContract.BiGGDBConstants.MCC_CC_ID + " AND\n" + "       "
      + BiGGDBContract.BiGGDBConstants.C_BIGG_ID + " = '%s' AND\n" + "       "
      + BiGGDBContract.BiGGDBConstants.M_BIGG_ID + " = '%s' AND\n" + "       "
      + BiGGDBContract.BiGGDBConstants.M_ID + " = "
      + BiGGDBContract.BiGGDBConstants.MCC_MODEL_ID + ";";
    return getString(query, biggId.getAbbreviation(), modelId);
  }


  /**
   * @param biggId
   * @return
   */
  public String getCompartmentName(BiGGId biggId) {
    return getString(
      "SELECT " + BiGGDBContract.BiGGDBConstants.NAME + " FROM "
        + BiGGDBContract.BiGGDBConstants.COMPARTMENT + " WHERE "
        + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public String getComponentName(BiGGId biggId) throws SQLException {
    return getString(
      "SELECT " + BiGGDBContract.BiGGDBConstants.NAME + " FROM "
        + BiGGDBContract.BiGGDBConstants.COMPONENT + " WHERE "
        + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @param includeAnyURI
   * @return a list of external source together with external id.
   * @throws SQLException
   */
  public List<String> getComponentResources(BiGGId biggId,
    boolean includeAnyURI) throws SQLException {
    return getResourceURL(biggId, BiGGDBContract.BiGGDBConstants.COMPONENT,
      includeAnyURI);
  }


  /**
   * @param biggId
   * @return
   */
  public String getComponentType(BiGGId biggId) {
    return getString(
      "SELECT " + BiGGDBContract.BiGGDBConstants.TYPE + " FROM "
        + BiGGDBContract.BiGGDBConstants.COMPONENT + " WHERE "
        + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
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
    String query = "SELECT " + BiGGDBContract.BiGGDBConstants.D_BIGG_ID + ", "
      + BiGGDBContract.BiGGDBConstants.S_SYNONYM + "\n" + "FROM  "
      + BiGGDBContract.BiGGDBConstants.DATA_SOURCE + ", "
      + BiGGDBContract.BiGGDBConstants.SYNONYM + ", "
      + BiGGDBContract.BiGGDBConstants.GENOME_REGION + "\n" + "WHERE "
      + BiGGDBContract.BiGGDBConstants.D_ID + " = "
      + BiGGDBContract.BiGGDBConstants.S_DATA_SOURCE_ID + " AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.S_OME_ID + " = "
      + BiGGDBContract.BiGGDBConstants.GR_ID + " AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.GR_BIGG_ID + " = '%s' AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.D_BIGG_ID + " != "
      + BiGGDBContract.BiGGDBConstants.OLD_BIGG_ID + " AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.D_BIGG_ID + " NOT LIKE "
      + BiGGDBContract.BiGGDBConstants.REFSEQ_PATTERN;
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
    String query = "SELECT " + BiGGDBContract.BiGGDBConstants.S_SYNONYM + "\n"
      + "FROM  " + BiGGDBContract.BiGGDBConstants.DATA_SOURCE + ", "
      + BiGGDBContract.BiGGDBConstants.SYNONYM + ", "
      + BiGGDBContract.BiGGDBConstants.GENOME_REGION + "\n" + "WHERE "
      + BiGGDBContract.BiGGDBConstants.D_ID + " = "
      + BiGGDBContract.BiGGDBConstants.S_DATA_SOURCE_ID + " AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.S_OME_ID + " = "
      + BiGGDBContract.BiGGDBConstants.GR_ID + " AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.GR_BIGG_ID + " = '%s' AND\n" + "      "
      + BiGGDBContract.BiGGDBConstants.D_BIGG_ID + " = "
      + BiGGDBContract.BiGGDBConstants.REFSEQ_NAME;
    return getString(query, label);
  }


  /**
   * @param reactionId
   * @param modelId
   * @return
   */
  public String getGeneReactionRule(String reactionId, String modelId) {
    return getString(
      "SELECT REPLACE(RTRIM(REPLACE(REPLACE("
        + BiGGDBContract.BiGGDBConstants.MR_GENE_REACTION_RULE
        + ", 'or', '||'), 'and', '&&'), '.'), '.', '__SBML_DOT__') AS "
        + BiGGDBContract.BiGGDBConstants.GENE_REACTION_RULE + " " + "FROM  "
        + BiGGDBContract.BiGGDBConstants.MODEl_REACTION + ", "
        + BiGGDBContract.BiGGDBConstants.REACTION + ", "
        + BiGGDBContract.BiGGDBConstants.MODEL + " " + "WHERE "
        + BiGGDBContract.BiGGDBConstants.R_ID + " = "
        + BiGGDBContract.BiGGDBConstants.MR_REACTION_ID + " AND " + "      "
        + BiGGDBContract.BiGGDBConstants.M_ID + " = "
        + BiGGDBContract.BiGGDBConstants.MR_MODEL_ID + " AND " + "      "
        + BiGGDBContract.BiGGDBConstants.MR_GENE_REACTION_RULE
        + " IS NOT NULL AND " + "      LENGTH("
        + BiGGDBContract.BiGGDBConstants.MR_GENE_REACTION_RULE + ") > 0 AND "
        + BiGGDBContract.BiGGDBConstants.R_BIGG_ID + " = '%s' AND " + "      "
        + BiGGDBContract.BiGGDBConstants.M_BIGG_ID + " = '%s'",
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
    ResultSet rst = connect.query(
      "SELECT " + BiGGDBContract.BiGGDBConstants.FIRST_CREATED + " FROM "
        + BiGGDBContract.BiGGDBConstants.MODEL + " WHERE "
        + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'",
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
    return getString("SELECT " + BiGGDBContract.BiGGDBConstants.DESCRIPTION
      + " FROM " + BiGGDBContract.BiGGDBConstants.MODEL + " WHERE "
      + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @return
   */
  public String getOrganism(String biggId) {
    return getString("SELECT " + BiGGDBContract.BiGGDBConstants.G_ORGANISM
      + " FROM " + BiGGDBContract.BiGGDBConstants.GENOME + ", "
      + BiGGDBContract.BiGGDBConstants.MODEL + " WHERE "
      + BiGGDBContract.BiGGDBConstants.M_GENOME_ID + " = "
      + BiGGDBContract.BiGGDBConstants.G_ID + " AND "
      + BiGGDBContract.BiGGDBConstants.M_BIGG_ID + " = '%s'", biggId);
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
    ResultSet rst =
      connect.query("SELECT " + BiGGDBContract.BiGGDBConstants.P_REFERENCE_TYPE
        + ", " + BiGGDBContract.BiGGDBConstants.P_REFERENCE_ID + " " + "FROM  "
        + BiGGDBContract.BiGGDBConstants.PUBLICATION + ", "
        + BiGGDBContract.BiGGDBConstants.PUBLICATION_MODEL + ", "
        + BiGGDBContract.BiGGDBConstants.MODEL + " " + "WHERE "
        + BiGGDBContract.BiGGDBConstants.P_ID + " = "
        + BiGGDBContract.BiGGDBConstants.PM_PUBLICATION_ID + " AND "
        + BiGGDBContract.BiGGDBConstants.PM_MODEL_ID + " = "
        + BiGGDBContract.BiGGDBConstants.M_ID + " AND "
        + BiGGDBContract.BiGGDBConstants.M_BIGG_ID + " = '%s'", biggId);
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
    return getString("SELECT " + BiGGDBContract.BiGGDBConstants.NAME + " FROM "
      + BiGGDBContract.BiGGDBConstants.REACTION + " WHERE "
      + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'", biggId);
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
    ResultSet rst = connect.query(
      "SELECT CONCAT(" + BiGGDBContract.BiGGDBConstants.URL_PREFIX + ", "
        + BiGGDBContract.BiGGDBConstants.S_SYNONYM + ") AS "
        + BiGGDBContract.BiGGDBConstants.URL + " " + "FROM  %s c, "
        + BiGGDBContract.BiGGDBConstants.SYNONYM + ", "
        + BiGGDBContract.BiGGDBConstants.DATA_SOURCE + " " + "WHERE "
        + BiGGDBContract.BiGGDBConstants.C_ID + " = "
        + BiGGDBContract.BiGGDBConstants.S_OME_ID + " AND" + "      "
        + BiGGDBContract.BiGGDBConstants.S_DATA_SOURCE_ID + " = "
        + BiGGDBContract.BiGGDBConstants.D_ID + " AND" + "      "
        + BiGGDBContract.BiGGDBConstants.URL_PREFIX + " IS NOT NULL AND"
        + "      " + BiGGDBContract.BiGGDBConstants.C_BIGG_ID + " = '%s'%s",
      type, biggId.getAbbreviation(),
      includeAnyURI ? "" : " AND " + BiGGDBContract.BiGGDBConstants.URL_PREFIX
        + " like '%%identifiers.org%%'");
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
      return getInt("SELECT " + BiGGDBContract.BiGGDBConstants.TAXON_ID
        + " FROM " + BiGGDBContract.BiGGDBConstants.GENOME + ", "
        + BiGGDBContract.BiGGDBConstants.MODEL + " WHERE "
        + BiGGDBContract.BiGGDBConstants.G_ID + " = "
        + BiGGDBContract.BiGGDBConstants.M_GENOME_ID + " AND "
        + BiGGDBContract.BiGGDBConstants.M_BIGG_ID + " = '%s'", biggId);
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
      return getInt(
        "SELECT COUNT(*) FROM " + BiGGDBContract.BiGGDBConstants.COMPARTMENT
          + " WHERE " + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'",
        biggId) > 0;
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
      return getInt(
        "SELECT COUNT(*) FROM " + BiGGDBContract.BiGGDBConstants.COMPONENT
          + " WHERE " + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'",
        biggId) > 0;
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
      return getInt(
        "SELECT COUNT(*) FROM " + BiGGDBContract.BiGGDBConstants.MODEL
          + " WHERE " + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'",
        biggId) > 0;
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
      return getInt(
        "SELECT COUNT(*) FROM " + BiGGDBContract.BiGGDBConstants.REACTION
          + " WHERE " + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'",
        biggId) > 0;
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
    String query = "SELECT " + BiGGDBContract.BiGGDBConstants.MCC_CHARGE + "\n"
      + "FROM   " + BiGGDBContract.BiGGDBConstants.COMPONENT + ",\n" + "       "
      + BiGGDBContract.BiGGDBConstants.COMPARTMENTALIZED_COMPONENT + ",\n"
      + "       " + BiGGDBContract.BiGGDBConstants.MODEL + ",\n" + "       "
      + BiGGDBContract.BiGGDBConstants.MCC + "\n" + "WHERE  "
      + BiGGDBContract.BiGGDBConstants.C_ID + " = "
      + BiGGDBContract.BiGGDBConstants.CC_COMPONENT_ID + " AND\n" + "       "
      + BiGGDBContract.BiGGDBConstants.CC_ID + " = "
      + BiGGDBContract.BiGGDBConstants.MCC_COMPARTMENTALIZED_COMPONENT_ID
      + " AND\n" + "       " + BiGGDBContract.BiGGDBConstants.C_BIGG_ID
      + " = '%s' AND\n" + "       " + BiGGDBContract.BiGGDBConstants.M_BIGG_ID
      + " = '%s' AND\n" + "       " + BiGGDBContract.BiGGDBConstants.M_ID
      + " = " + BiGGDBContract.BiGGDBConstants.MCC_MODEL_ID;
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
    String query = "SELECT " + BiGGDBContract.BiGGDBConstants.PSEUDOREACTION
      + " FROM " + BiGGDBContract.BiGGDBConstants.REACTION + " WHERE "
      + BiGGDBContract.BiGGDBConstants.BIGG_ID + " = '%s'";
    String result = getString(query,
      reactionId.startsWith("R_") ? reactionId.substring(2) : reactionId);
    return (result != null) && result.equals("t");
  }
}
