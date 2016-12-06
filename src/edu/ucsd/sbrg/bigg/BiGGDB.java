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
      return getDate("SELECT " + BiGGDBContract.Constants.DATE_TIME + " FROM "
        + BiGGDBContract.Constants.DATABASE_VERSION);
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
    String query = "SELECT DISTINCT " + BiGGDBContract.Constants.MR_SUBSYSTEM
      + "\n" + "FROM " + BiGGDBContract.Constants.REACTION_R + ", "
      + BiGGDBContract.Constants.MODEL_M + ", "
      + BiGGDBContract.Constants.MODEL_REACTION + "\n" + "WHERE "
      + BiGGDBContract.Constants.M_BIGG_ID + " = '%s' AND\n" + "      "
      + BiGGDBContract.Constants.R_BIGG_ID + " = '%s' AND\n" + "      "
      + BiGGDBContract.Constants.M_ID + " = "
      + BiGGDBContract.Constants.MR_MODEL_ID + " AND\n" + "      "
      + BiGGDBContract.Constants.R_ID + " = "
      + BiGGDBContract.Constants.MR_REACTION_ID + " AND\n" + "      length("
      + BiGGDBContract.Constants.MR_SUBSYSTEM + ") > 0";
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
    String query = "SELECT " + BiGGDBContract.Constants.MCC_FORMULA + "\n"
      + "FROM   " + BiGGDBContract.Constants.COMPONENT_C + ",\n" + "       "
      + BiGGDBContract.Constants.COMPARTMENTALIZED_COMPONENT + ",\n" + "       "
      + BiGGDBContract.Constants.MODEL_M + ",\n" + "       "
      + BiGGDBContract.Constants.MCC + "\n" + "WHERE  "
      + BiGGDBContract.Constants.C_ID + " = "
      + BiGGDBContract.Constants.CC_COMPONENT_ID + " AND\n" + "       "
      + BiGGDBContract.Constants.CC_ID + " = "
      + BiGGDBContract.Constants.MCC_COMPARTMENTALIZED_COMPONENT_ID + " AND\n"
      + "       " + BiGGDBContract.Constants.C_BIGG_ID + " = '%s' AND\n"
      + "       " + BiGGDBContract.Constants.M_BIGG_ID + " = '%s' AND\n"
      + "       " + BiGGDBContract.Constants.M_ID + " = "
      + BiGGDBContract.Constants.MCC_MODEL_ID + ";";
    return getString(query, biggId.getAbbreviation(), modelId);
  }


  /**
   * @param biggId
   * @return
   */
  public String getCompartmentName(BiGGId biggId) {
    return getString(
      "SELECT " + BiGGDBContract.Constants.NAME + " FROM "
        + BiGGDBContract.Constants.COMPARTMENT + " WHERE "
        + BiGGDBContract.Constants.BIGG_ID + " = '%s'",
      biggId.getAbbreviation());
  }


  /**
   * @param biggId
   * @return
   * @throws SQLException
   */
  public String getComponentName(BiGGId biggId) throws SQLException {
    return getString(
      "SELECT " + BiGGDBContract.Constants.NAME + " FROM "
        + BiGGDBContract.Constants.COMPONENT + " WHERE "
        + BiGGDBContract.Constants.BIGG_ID + " = '%s'",
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
    return getResourceURL(biggId, BiGGDBContract.Constants.COMPONENT,
      includeAnyURI);
  }


  /**
   * @param biggId
   * @return
   */
  public String getComponentType(BiGGId biggId) {
    return getString(
      "SELECT " + BiGGDBContract.Constants.TYPE + " FROM "
        + BiGGDBContract.Constants.COMPONENT + " WHERE "
        + BiGGDBContract.Constants.BIGG_ID + " = '%s'",
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
    String query = "SELECT " + BiGGDBContract.Constants.D_BIGG_ID + ", "
      + BiGGDBContract.Constants.S_SYNONYM + "\n" + "FROM  "
      + BiGGDBContract.Constants.DATA_SOURCE + ", "
      + BiGGDBContract.Constants.SYNONYM + ", "
      + BiGGDBContract.Constants.GENOME_REGION + "\n" + "WHERE "
      + BiGGDBContract.Constants.D_ID + " = "
      + BiGGDBContract.Constants.S_DATA_SOURCE_ID + " AND\n" + "      "
      + BiGGDBContract.Constants.S_OME_ID + " = "
      + BiGGDBContract.Constants.GR_ID + " AND\n" + "      "
      + BiGGDBContract.Constants.GR_BIGG_ID + " = '%s' AND\n" + "      "
      + BiGGDBContract.Constants.D_BIGG_ID + " != "
      + BiGGDBContract.Constants.OLD_BIGG_ID + " AND\n" + "      "
      + BiGGDBContract.Constants.D_BIGG_ID + " NOT LIKE "
      + BiGGDBContract.Constants.REFSEQ_PATTERN;
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
    String query = "SELECT " + BiGGDBContract.Constants.S_SYNONYM + "\n"
      + "FROM  " + BiGGDBContract.Constants.DATA_SOURCE + ", "
      + BiGGDBContract.Constants.SYNONYM + ", "
      + BiGGDBContract.Constants.GENOME_REGION + "\n" + "WHERE "
      + BiGGDBContract.Constants.D_ID + " = "
      + BiGGDBContract.Constants.S_DATA_SOURCE_ID + " AND\n" + "      "
      + BiGGDBContract.Constants.S_OME_ID + " = "
      + BiGGDBContract.Constants.GR_ID + " AND\n" + "      "
      + BiGGDBContract.Constants.GR_BIGG_ID + " = '%s' AND\n" + "      "
      + BiGGDBContract.Constants.D_BIGG_ID + " = "
      + BiGGDBContract.Constants.REFSEQ_NAME;
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
        + BiGGDBContract.Constants.MR_GENE_REACTION_RULE
        + ", 'or', '||'), 'and', '&&'), '.'), '.', '__SBML_DOT__') AS "
        + BiGGDBContract.Constants.GENE_REACTION_RULE + " " + "FROM  "
        + BiGGDBContract.Constants.MODEL_REACTION + ", "
        + BiGGDBContract.Constants.REACTION_R + ", "
        + BiGGDBContract.Constants.MODEL_M + " " + "WHERE "
        + BiGGDBContract.Constants.R_ID + " = "
        + BiGGDBContract.Constants.MR_REACTION_ID + " AND " + "      "
        + BiGGDBContract.Constants.M_ID + " = "
        + BiGGDBContract.Constants.MR_MODEL_ID + " AND " + "      "
        + BiGGDBContract.Constants.MR_GENE_REACTION_RULE + " IS NOT NULL AND "
        + "      LENGTH(" + BiGGDBContract.Constants.MR_GENE_REACTION_RULE
        + ") > 0 AND " + BiGGDBContract.Constants.R_BIGG_ID + " = '%s' AND "
        + "      " + BiGGDBContract.Constants.M_BIGG_ID + " = '%s'",
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
      "SELECT " + BiGGDBContract.Constants.FIRST_CREATED + " FROM "
        + BiGGDBContract.Constants.MODEL + " WHERE "
        + BiGGDBContract.Constants.BIGG_ID + " = '%s'",
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
    return getString("SELECT " + BiGGDBContract.Constants.DESCRIPTION + " FROM "
      + BiGGDBContract.Constants.MODEL + " WHERE "
      + BiGGDBContract.Constants.BIGG_ID + " = '%s'", biggId);
  }


  /**
   * @param biggId
   * @return
   */
  public String getOrganism(String biggId) {
    return getString("SELECT " + BiGGDBContract.Constants.MODEL_ORGANISM + " FROM "
      + BiGGDBContract.Constants.MODEL + " WHERE "
      + BiGGDBContract.Constants.MODEL_BIGG_ID + " = '%s'", biggId);
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
      connect.query("SELECT " + BiGGDBContract.Constants.P_REFERENCE_TYPE + ", "
        + BiGGDBContract.Constants.P_REFERENCE_ID + " " + "FROM  "
        + BiGGDBContract.Constants.PUBLICATION + ", "
        + BiGGDBContract.Constants.PUBLICATION_MODEL + ", "
        + BiGGDBContract.Constants.MODEL_M + " " + "WHERE "
        + BiGGDBContract.Constants.P_ID + " = "
        + BiGGDBContract.Constants.PM_PUBLICATION_ID + " AND "
        + BiGGDBContract.Constants.PM_MODEL_ID + " = "
        + BiGGDBContract.Constants.M_ID + " AND "
        + BiGGDBContract.Constants.M_BIGG_ID + " = '%s'", biggId);
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
    return getString("SELECT " + BiGGDBContract.Constants.NAME + " FROM "
      + BiGGDBContract.Constants.REACTION + " WHERE "
      + BiGGDBContract.Constants.BIGG_ID + " = '%s'", biggId);
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
      "SELECT CONCAT(" + BiGGDBContract.Constants.URL_PREFIX + ", "
        + BiGGDBContract.Constants.S_SYNONYM + ") AS "
        + BiGGDBContract.Constants.URL + " " + "FROM  %s c, "
        + BiGGDBContract.Constants.SYNONYM + ", "
        + BiGGDBContract.Constants.DATA_SOURCE + " " + "WHERE "
        + BiGGDBContract.Constants.C_ID + " = "
        + BiGGDBContract.Constants.S_OME_ID + " AND" + "      "
        + BiGGDBContract.Constants.S_DATA_SOURCE_ID + " = "
        + BiGGDBContract.Constants.D_ID + " AND" + "      "
        + BiGGDBContract.Constants.URL_PREFIX + " IS NOT NULL AND" + "      "
        + BiGGDBContract.Constants.C_BIGG_ID + " = '%s'%s",
      type, biggId.getAbbreviation(), includeAnyURI ? "" : " AND "
        + BiGGDBContract.Constants.URL_PREFIX + " like '%%identifiers.org%%'");
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
      return getInt("SELECT " + BiGGDBContract.Constants.TAXON_ID + " FROM "
        + BiGGDBContract.Constants.GENOME + ", "
        + BiGGDBContract.Constants.MODEL_M + " WHERE "
        + BiGGDBContract.Constants.G_ID + " = "
        + BiGGDBContract.Constants.M_GENOME_ID + " AND "
        + BiGGDBContract.Constants.M_BIGG_ID + " = '%s'", biggId);
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
        "SELECT COUNT(*) FROM " + BiGGDBContract.Constants.COMPARTMENT
          + " WHERE " + BiGGDBContract.Constants.BIGG_ID + " = '%s'",
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
      return getInt("SELECT COUNT(*) FROM " + BiGGDBContract.Constants.COMPONENT
        + " WHERE " + BiGGDBContract.Constants.BIGG_ID + " = '%s'", biggId) > 0;
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
      return getInt("SELECT COUNT(*) FROM " + BiGGDBContract.Constants.MODEL
        + " WHERE " + BiGGDBContract.Constants.BIGG_ID + " = '%s'", biggId) > 0;
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
      return getInt("SELECT COUNT(*) FROM " + BiGGDBContract.Constants.REACTION
        + " WHERE " + BiGGDBContract.Constants.BIGG_ID + " = '%s'", biggId) > 0;
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
    String query = "SELECT " + BiGGDBContract.Constants.MCC_CHARGE + "\n"
      + "FROM   " + BiGGDBContract.Constants.COMPONENT_C + ",\n" + "       "
      + BiGGDBContract.Constants.COMPARTMENTALIZED_COMPONENT + ",\n" + "       "
      + BiGGDBContract.Constants.MODEL_M + ",\n" + "       "
      + BiGGDBContract.Constants.MCC + "\n" + "WHERE  "
      + BiGGDBContract.Constants.C_ID + " = "
      + BiGGDBContract.Constants.CC_COMPONENT_ID + " AND\n" + "       "
      + BiGGDBContract.Constants.CC_ID + " = "
      + BiGGDBContract.Constants.MCC_COMPARTMENTALIZED_COMPONENT_ID + " AND\n"
      + "       " + BiGGDBContract.Constants.C_BIGG_ID + " = '%s' AND\n"
      + "       " + BiGGDBContract.Constants.M_BIGG_ID + " = '%s' AND\n"
      + "       " + BiGGDBContract.Constants.M_ID + " = "
      + BiGGDBContract.Constants.MCC_MODEL_ID;
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
    String query = "SELECT " + BiGGDBContract.Constants.PSEUDOREACTION
      + " FROM " + BiGGDBContract.Constants.REACTION + " WHERE "
      + BiGGDBContract.Constants.BIGG_ID + " = '%s'";
    String result = getString(query,
      reactionId.startsWith("R_") ? reactionId.substring(2) : reactionId);
    return (result != null) && result.equals("t");
  }
}
