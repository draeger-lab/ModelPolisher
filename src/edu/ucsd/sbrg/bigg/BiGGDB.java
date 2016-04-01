/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of the program NetBuilder.
 *
 * Copyright (C) 2013 by the University of California, San Diego.
 *
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
 *
 */
public class BiGGDB {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(BiGGDB.class.getName());

  /**
   * The connection to the database.
   */
  private PostgreSQLConnector conect;

  /**
   * @param connector
   * @throws SQLException
   * 
   */
  public BiGGDB(PostgreSQLConnector connector) throws SQLException {
    conect = connector;
    if (!connector.isConnected()) {
      connector.connect();
    }
  }

  /**
   * 
   * @return
   */
  public Date getBiGGVersion() {
    try {
      return getDate("SELECT date_time FROM database_version");
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    return null;
  }

  /**
   * 
   * @param modelBiGGid
   * @param reactionBiGGid
   * @return
   */
  public List<String> getSubsystems(String modelBiGGid, String reactionBiGGid) {
    String query = "SELECT DISTINCT mr.subsystem\n"
        + "FROM  reaction r, model m, model_reaction mr\n"
        + "WHERE m.bigg_id = '%s' AND\n"
        + "      r.bigg_id = '%s' AND\n"
        + "      m.id = mr.model_id AND\n"
        + "      r.id = mr.reaction_id AND\n"
        + "      length(mr.subsystem) > 0";
    List<String> list = new LinkedList<String>();
    try {
      ResultSet rst = conect.query(query, modelBiGGid, reactionBiGGid);
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
   * 
   * @param biggId
   * @return
   */
  public String getChemicalFormula(BiGGId biggId, String modelId) {
    String query = "SELECT TRIM(LTRIM(m.formula, '['''), ''']') FROM metabolite m, component c WHERE c.id = m.id AND c.bigg_id = '%s'";
    query = "SELECT mcc.formula\n" +
        "FROM   component c,\n" +
        "       compartmentalized_component cc,\n" +
        "       model m,\n" +
        "       model_compartmentalized_component mcc\n" +
        "WHERE  c.id = cc.component_id AND\n" +
        "       cc.id = mcc.compartmentalized_component_id AND\n" +
        "       c.bigg_id = '%s' AND\n" +
        "       m.bigg_id = '%s' AND\n" +
        "       m.id = mcc.model_id;";
    return getString(query, biggId.getAbbreviation(), modelId);
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public String getCompartmentName(BiGGId biggId) {
    return getString("SELECT name FROM compartment WHERE bigg_id = '%s'", biggId.getAbbreviation());
  }

  /**
   * 
   * @param biggId
   * @return
   * @throws SQLException
   */
  public String getComponentName(BiGGId biggId) throws SQLException {
    return getString("SELECT name FROM component WHERE bigg_id = '%s'", biggId.getAbbreviation());
  }

  /**
   * 
   * @param biggId
   * @param includeAnyURI
   * @return a list of external source together with external id.
   * @throws SQLException
   */
  public List<String> getComponentResources(BiGGId biggId, boolean includeAnyURI) throws SQLException {
    return getResourceURL(biggId, "component", includeAnyURI);
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public String getComponentType(BiGGId biggId) {
    return getString("SELECT type FROM component WHERE bigg_id = '%s'", biggId.getAbbreviation());
  }

  /**
   * 
   * @param query
   * @return
   * @throws SQLException
   */
  private Date getDate(String query) throws SQLException {
    ResultSet rst = conect.query(query);
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
    List<Pair<String, String>> list = new LinkedList<Pair<String,String>>();
    String query = "SELECT d.bigg_id, s.synonym\n"
        + "FROM  data_source d, synonym s, genome_region gr\n"
        + "WHERE d.id = s.data_source_id AND\n"
        + "      s.ome_id = gr.id AND\n"
        + "      gr.bigg_id = '%s' AND\n"
        + "      d.bigg_id != 'old_bigg_id' AND\n"
        + "      d.bigg_id NOT LIKE 'refseq_%%'";
    try {
      ResultSet rst = conect.query(query, label);
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
   * 
   * @param label
   * @return
   */
  public String getGeneName(String label) {
    String query = "SELECT s.synonym\n"
        + "FROM  data_source d, synonym s, genome_region gr\n"
        + "WHERE d.id = s.data_source_id AND\n"
        + "      s.ome_id = gr.id AND\n"
        + "      gr.bigg_id = '%s' AND\n"
        + "      d.bigg_id = 'refseq_name'";
    return getString(query, label);
  }

  /**
   * 
   * @param reactionId
   * @param modelId
   * @return
   */
  public String getGeneReactionRule(String reactionId, String modelId) {
    return getString("SELECT REPLACE(RTRIM(REPLACE(REPLACE(mr.gene_reaction_rule, 'or', '||'), 'and', '&&'), '.'), '.', '__SBML_DOT__') AS gene_reaction_rule " +
        "FROM  model_reaction mr, reaction r, model m " +
        "WHERE r.id = mr.reaction_id AND " +
        "      m.id = mr.model_id AND " +
        "      mr.gene_reaction_rule IS NOT NULL AND " +
        "      LENGTH(mr.gene_reaction_rule) > 0 AND r.bigg_id = '%s' AND " +
        "      m.bigg_id = '%s'", reactionId, modelId);
  }

  /**
   * 
   * @param query
   * @param args
   * @return
   * @throws SQLException
   */
  private Integer getInt(String query, Object... args) throws SQLException {
    ResultSet rst = conect.query(query, args);
    Integer result = rst.next() ? result = rst.getInt(1) : null;
    rst.getStatement().close();
    return result;
  }

  /**
   * Do not use!
   * @param biggId
   * @return
   * @throws SQLException
   */
  public Date getModelCreationDate(BiGGId biggId) throws SQLException {
    ResultSet rst = conect.query("SELECT first_created FROM model WHERE bigg_id = '%s'", biggId.getAbbreviation());
    Date result = rst.next() ? rst.getDate(1) : null;
    rst.getStatement().close();
    return result;
  }

  /**
   * 
   * @param biggId
   * @return
   * @throws SQLException
   */
  public String getModelDescription(String biggId) throws SQLException {
    return getString("SELECT description FROM model WHERE bigg_id = '%s'", biggId);
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public String getOrganism(String biggId) {
    return getString("SELECT g.organism FROM genome g, model m WHERE m.genome_id = g.id AND m.bigg_id = '%s'", biggId);
  }

  /**
   * 
   * @param biggId
   * @return
   * @throws SQLException
   */
  public List<Pair<String, String>> getPublications(BiGGId biggId) throws SQLException {
    return getPublications(biggId.getAbbreviation());
  }

  /**
   * 
   * @param biggId
   * @return
   * @throws SQLException
   */
  public List<Pair<String, String>> getPublications(String biggId) throws SQLException {
    ResultSet rst = conect.query("SELECT p.reference_type, p.reference_id " +
        "FROM  publication p, publication_model pm, model m " +
        "WHERE p.id = pm.publication_id AND pm.model_id = m.id AND m.bigg_id = '%s'", biggId);
    List<Pair<String, String>> list = new LinkedList<>();
    while (rst.next()) {
      String key = rst.getString(1);
      list.add(pairOf(key.equals("pmid") ? "pubmed" : key, rst.getString(2)));
    }
    rst.getStatement().close();
    return list;
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public String getReactionName(String biggId) {
    return getString("SELECT name FROM reaction WHERE bigg_id = '%s'", biggId);
  }

  /**
   * 
   * @param biggId
   * @param type
   * @param includeAnyURI
   * @return a list of external source together with external id.
   * @throws SQLException
   */
  private List<String> getResourceURL(BiGGId biggId, String type, boolean includeAnyURI) throws SQLException {
    ResultSet rst = conect.query("SELECT CONCAT(url_prefix, s.synonym) AS url " +
        "FROM  %s c, synonym s, data_source d " +
        "WHERE c.id = s.ome_id AND" +
        "      s.data_source_id = d.id AND" +
        "      url_prefix IS NOT NULL AND" +
        "      c.bigg_id = '%s'%s",
        type,
        biggId.getAbbreviation(),
        includeAnyURI ? "" : " AND url_prefix like '%%identifiers.org%%'");
    List<String> result = new LinkedList<String>();
    while (rst.next()) {
      result.add(rst.getString(1));
    }
    rst.getStatement().close();
    return result;
  }

  /**
   * 
   * @param query
   * @param args
   * @return
   */
  public String getString(String query, Object... args) {
    try {
      ResultSet rst = conect.query(query, args);
      String result = rst.next() ? rst.getString(1) : "";
      rst.getStatement().close();
      return result;
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return "";
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public Integer getTaxonId(String biggId) {
    try {
      return getInt("SELECT taxon_id FROM genome g, model m WHERE g.id = m.genome_id AND m.bigg_id = '%s'", biggId);
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not retrieve NCBI taxon identifier for model ''{0}'', because of {1}.",
        biggId, Utils.getMessage(exc)));
    }
    return null;
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public boolean isCompartment(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM compartment WHERE bigg_id = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not determine if ''{0}'' is a compartment or not: {1}.",
        biggId, Utils.getMessage(exc)));
    }
    return false;
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public boolean isMetabolite(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM component WHERE bigg_id = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not determine if ''{0}'' is a metabolite or not: {1}.",
        biggId, Utils.getMessage(exc)));
    }
    return false;
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public boolean isModel(String biggId) {
    try {
      return getInt("SELECT COUNT(*) FROM model WHERE bigg_id = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not determine if ''{0}'' is a model or not: {1}.",
        biggId, Utils.getMessage(exc)));
    }
    return false;
  }

  /**
   * 
   * @param biggId
   * @return
   */
  public boolean isReaction(String biggId) {
    if (biggId.startsWith("R_")) {
      biggId = biggId.substring(2);
    }
    try {
      return getInt("SELECT COUNT(*) FROM reaction WHERE bigg_id = '%s'", biggId) > 0;
    } catch (SQLException exc) {
      logger.warning(MessageFormat.format(
        "Could not determine if ''{0}'' is a reaction or not: {1}.",
        biggId, Utils.getMessage(exc)));
    }
    return false;
  }

  /**
   * 
   * @param biggId
   * @param modelId
   * @return
   */
  public Integer getCharge(String biggId, String modelId) {
    String query = "SELECT mcc.charge\n" +
        "FROM   component c,\n" +
        "       compartmentalized_component cc,\n" +
        "       model m,\n" +
        "       model_compartmentalized_component mcc\n" +
        "WHERE  c.id = cc.component_id AND\n" +
        "       cc.id = mcc.compartmentalized_component_id AND\n" +
        "       c.bigg_id = '%s' AND\n" +
        "       m.bigg_id = '%s' AND\n" +
        "       m.id = mcc.model_id";
    String charge = getString(query, biggId, modelId);
    if ((charge == null) || (charge.trim().length() == 0)) {
      return null;
    }
    return charge != null ? Integer.valueOf(Integer.parseInt(charge)) : null;
  }

  /**
   * 
   * @param reactionId
   * @return
   */
  public boolean isPseudoreaction(String reactionId) {
    String query = "SELECT pseudoreaction FROM reaction WHERE bigg_id = '%s'";
    String result = getString(query, reactionId.startsWith("R_") ? reactionId.substring(2) : reactionId);
    return (result != null) && result.equals("t");
  }

}
