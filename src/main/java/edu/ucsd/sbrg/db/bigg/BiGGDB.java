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
package edu.ucsd.sbrg.db.bigg;

import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import edu.ucsd.sbrg.db.PostgreSQLConnector;
import edu.ucsd.sbrg.miriam.Registry;
import org.sbml.jsbml.util.Pair;

import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.db.bigg.BiGGDBContract.Constants.Column.*;
import static edu.ucsd.sbrg.db.bigg.BiGGDBContract.Constants.*;
import static edu.ucsd.sbrg.db.bigg.BiGGDBContract.Constants.Table.*;
import static java.text.MessageFormat.format;
import static org.sbml.jsbml.util.Pair.pairOf;

/**
 * This class provides a connection to the BiGG database.
 * 
 * @author Andreas Dr&auml;ger
 */
public class BiGGDB {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(BiGGDB.class.getName());
  /**
   * Bundle for ModelPolisher logger messages
   */
  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   * The connection to the database.
   */
  private static PostgreSQLConnector connector;

  /**
   * Don't allow instantiation
   */
  private BiGGDB() {
  }


  /**
   * @param host
   * @param port
   * @param user
   * @param passwd
   * @param name
   */
  public static void init(String host, String port, String user, String passwd, String name) {
    connector = new PostgreSQLConnector(host, Integer.parseInt(port), user, passwd, name);
  }



  public static void close() {
    connector.close();
  }


  /**
   * @return True if connector is initialized
   */
  public static boolean inUse() {
    return connector != null;
  }


  /**
   * Retrieves the version date of the BiGG database.
   * 
   * This method queries the database to fetch the date and time of the last update
   * from the DATABASE_VERSION table. It returns an {@link Optional} containing the
   * date if the query is successful and the date exists, otherwise it returns an empty {@link Optional}.
   * 
   * @return {@link Optional<Date>} The date of the last database update, or an empty {@link Optional} if not available.
   */
  public static Optional<Date> getBiGGVersion() {
    Optional<Date> date = Optional.empty();
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement("SELECT " + DATE_TIME + " FROM " + DATABASE_VERSION);
      ResultSet resultSet = pStatement.executeQuery();
      if (resultSet.next()) {
        date = Optional.of(resultSet.getDate(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.finest(format("{0}: {1}", exc.getClass().getName(), Utils.getMessage(exc)));
    }
    return date;
  }


  /**
   * Retrieves a list of distinct subsystems associated with a specific model and reaction BiGG IDs.
   * This method executes a SQL query to fetch subsystems from the database where both the model and reaction
   * match the provided BiGG IDs. Only subsystems with a non-zero length are considered.
   * 
   * @param modelBiGGid The BiGG ID of the model.
   * @param reactionBiGGid The BiGG ID of the reaction.
   * @return A List of subsystem names as strings. Returns an empty list if no subsystems are found or if an error occurs.
   */
  public static List<String> getSubsystems(String modelBiGGid, String reactionBiGGid) {
    String query = "SELECT DISTINCT mr." + SUBSYSTEM + " FROM " + REACTION + " r, " + MODEL + " m, " + MODEL_REACTION
      + " mr WHERE m." + BIGG_ID + " = ? AND r." + BIGG_ID + " = ? AND m." + ID + " = mr." + MODEL_ID + " AND r." + ID
      + " = mr." + REACTION_ID + " AND LENGTH(mr." + SUBSYSTEM + ") > 0";
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
   * Retrieves a list of distinct subsystems associated with a specific reaction BiGG ID.
   * This method executes a SQL query to fetch subsystems from the database where the reaction
   * matches the provided BiGG ID. Only subsystems with a non-zero length are considered.
   * 
   * @param reactionBiGGid The BiGG ID of the reaction for which subsystems are to be retrieved.
   * @return A List of subsystem names as strings. Returns an empty list if no subsystems are found or if an error occurs.
   */
  public static List<String> getSubsystemsForReaction(String reactionBiGGid) {
    String query = "SELECT DISTINCT mr." + SUBSYSTEM + " FROM " + REACTION + " r, " + MODEL_REACTION + " mr WHERE r."
      + BIGG_ID + " = ? AND r." + ID + " = mr." + REACTION_ID + " AND LENGTH(mr." + SUBSYSTEM + ") > 0";
    List<String> list = new LinkedList<>();
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, reactionBiGGid);
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
   * Retrieves the unique chemical formula for a given component within a specific compartment.
   * This method queries the database to find the distinct chemical formula associated with the specified
   * component and compartment IDs. It ensures that the formula is unique for the given parameters.
   * If multiple distinct formulas are found, it logs a warning indicating ambiguity.
   *
   * @param componentId The BiGG ID of the component for which the chemical formula is to be retrieved.
   * @param compartmentId The BiGG ID of the compartment where the component is located.
   * @return An {@link Optional} containing the chemical formula if exactly one unique formula is found,
   *         otherwise an empty {@link Optional} if none or multiple formulas are found.
   */
  public static Optional<String> getChemicalFormulaByCompartment(String componentId, String compartmentId) {
    String query = "SELECT DISTINCT mcc." + FORMULA + " FROM " + MCC + " mcc, " + COMPARTMENTALIZED_COMPONENT + " cc, "
      + COMPONENT + " c, " + COMPARTMENT + " co WHERE c." + BIGG_ID + " = ? AND c." + ID + " = cc." + COMPONENT_ID
      + " AND co." + BIGG_ID + " = ? AND co." + ID + " = cc." + COMPARTMENT_ID + " and cc." + ID + " = mcc."
      + COMPARTMENTALIZED_COMPONENT_ID + " AND mcc." + FORMULA + " <> '' ORDER BY mcc." + FORMULA;
    Set<String> results = runFormulaQuery(query, componentId, compartmentId);
    if (results.size() == 1) {
      return Optional.of(results.iterator().next());
    } else {
      if (results.size() > 1) {
        logger.info(format(MESSAGES.getString("FORMULA_COMPARTMENT_AMBIGUOUS"), componentId, compartmentId));
      }
      return Optional.empty();
    }
  }


  /**
   * Executes a database query to retrieve distinct chemical formulas based on the provided SQL query.
   * This method is designed to handle queries that fetch chemical formulas for a specific component
   * within either a compartment or a model, depending on the IDs provided.
   *
   * @param query The SQL query string that retrieves distinct chemical formulas.
   * @param componentId The BiGG ID of the component for which the formula is being retrieved.
   * @param compartmentOrModelId The BiGG ID of either the compartment or the model associated with the component.
   * @return A set of unique chemical formulas as strings. If no valid formulas are found, returns an empty set.
   */
  private static Set<String> runFormulaQuery(String query, String componentId, String compartmentOrModelId) {
    Set<String> results = new HashSet<>();
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, componentId);
      pStatement.setString(2, compartmentOrModelId);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        results.add(resultSet.getString(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return results.stream().filter(formula -> formula != null && !formula.isEmpty()).collect(Collectors.toSet());
  }


  /**
   * Retrieves the chemical formula for a given component within a specific model in the BiGG database.
   * This method executes a SQL query to find distinct chemical formulas associated with the component and model IDs provided.
   * If exactly one unique formula is found, it is returned. If none or multiple formulas are found, an empty Optional is returned.
   * In case of multiple formulas, a log entry is made indicating the ambiguity.
   *
   * @param componentId The BiGG ID of the component for which the formula is being retrieved.
   * @param modelId The BiGG ID of the model in which the component is present.
   * @return An {@link Optional<String>} containing the chemical formula if exactly one is found, otherwise empty.
   */
  public static Optional<String> getChemicalFormula(String componentId, String modelId) {
    String query = "SELECT DISTINCT mcc." + FORMULA + "\n FROM " + COMPONENT + " c,\n" + COMPARTMENTALIZED_COMPONENT
      + " cc,\n" + MODEL + " m,\n" + MCC + " mcc\n WHERE c." + ID + " = cc." + COMPONENT_ID + " AND\n cc." + ID
      + " = mcc." + COMPARTMENTALIZED_COMPONENT_ID + " AND\n c." + BIGG_ID + " = ? AND\n m." + BIGG_ID + " = ? AND\n m."
      + ID + " = mcc." + MODEL_ID + " AND mcc." + FORMULA + " <> ''";
    Set<String> results = runFormulaQuery(query, componentId, modelId);
    if (results.size() == 1) {
      return Optional.of(results.iterator().next());
    } else {
      if (results.size() > 1) {
        logger.info(format(MESSAGES.getString("FORMULA_MODEL_AMBIGUOUS"), componentId, modelId));
      }
      return Optional.empty();
    }
  }


  /**
   * Retrieves the name of the compartment associated with the given BiGG ID from the database.
   * This method constructs a SQL query to select the compartment name where the BiGG ID matches
   * and the name is not an empty string.
   *
   * @param biggId The BiGGId object containing the abbreviation of the compartment.
   * @return An {@link Optional<String>} containing the name of the compartment if found, otherwise empty.
   */
  public static Optional<String> getCompartmentName(BiGGId biggId) {
    String query = "SELECT " + NAME + " FROM " + COMPARTMENT + " WHERE " + BIGG_ID + " = ? AND " + NAME + " <> ''";
    return singleParamStatement(query, biggId.getAbbreviation());
  }


  /**
   * Executes a SQL query with a single parameter and returns the result as an Optional.
   * This method is designed to handle queries that are expected to return a single result.
   * If multiple results are found, a severe log is recorded indicating the issue.
   * 
   * @param query The SQL query to be executed. It should contain exactly one placeholder for the parameter.
   * @param param The parameter value to be used in the SQL query.
   * @return An {@link Optional<String>} containing the result if exactly one result is found, otherwise empty.
   */
  public static Optional<String> singleParamStatement(String query, String param) {
    Set<String> results = new HashSet<>();
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, param);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        results.add(resultSet.getString(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    results = results.stream().filter(result -> result != null && !result.isEmpty()).collect(Collectors.toSet());
    if (results.size() == 1) {
      return Optional.of(results.iterator().next());
    } else {
      if (results.size() > 1) {
        logger.severe(format(MESSAGES.getString("QUERY_MULTIPLE_RESULTS"), param, query));
      }
      return Optional.empty();
    }
  }


  /**
   * Retrieves the name of the component associated with the given BiGG ID from the database.
   * This method constructs a SQL query to select the component name where the BiGG ID matches
   * and the name is not an empty string.
   *
   * @param biggId The BiGGId object containing the abbreviation of the component.
   * @return An {@link Optional<String>} containing the name of the component if found, otherwise empty.
   */
  public static Optional<String> getComponentName(BiGGId biggId) {
    String query = "SELECT " + NAME + " FROM " + COMPONENT + " WHERE " + BIGG_ID + " = ? AND " + NAME + " <> ''";
    return singleParamStatement(query, biggId.getAbbreviation());
  }

  /**
   * Retrieves the type of the component associated with the given BiGG ID from the database.
   * This method constructs a SQL query to select the component type where the BiGG ID matches
   * and the name field is not an empty string.
   *
   * @param biggId The BiGGId object containing the abbreviation of the component.
   * @return An {@link Optional<String>} containing the type of the component if found, otherwise empty.
   */
  public static Optional<String> getComponentType(BiGGId biggId) {
    String query = "SELECT " + TYPE + " FROM " + COMPONENT + " WHERE " + BIGG_ID + " = ? AND " + NAME + " <> ''";
    return singleParamStatement(query, biggId.getAbbreviation());
  }


  /**
   * Retrieves all possible MIRIAM-compliant gene identifiers from the database based on a given label.
   * This method queries the database for gene identifiers that match the provided label and are compliant
   * with MIRIAM standards. Non-compliant entries are ignored.
   *
   * @param label The label used to query gene identifiers.
   * @return A TreeSet containing unique, sorted MIRIAM-compliant gene identifiers.
   */
  public static TreeSet<String> getGeneIds(String label) {
    TreeSet<String> results = new TreeSet<>();
    String query = "SELECT " + URL_PREFIX + ", s." + SYNONYM + "\n" + "FROM  " + DATA_SOURCE + " d, " + SYNONYM + " s, "
      + GENOME_REGION + " gr\n" + "WHERE d." + ID + " = s." + DATA_SOURCE_ID + " AND\n s." + OME_ID + " = gr." + ID
      + " AND\n gr." + BIGG_ID + " = ? AND\n d." + BIGG_ID + " != " + OLD_BIGG_ID + " AND\n d." + BIGG_ID + " NOT LIKE "
      + REFSEQ_PATTERN;
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
          resource = collection.replaceAll("http://", "https://") + identifier;
        } else if (collection == null) {
          logger.fine(MESSAGES.getString("COLLECTION_NULL_GENE"));
          continue;
        } else {
          logger.warning(format(MESSAGES.getString("IDENTIFIER_NULL_GENE"), collection));
          continue;
        }
        Registry.checkResourceUrl(resource).map(results::add);
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return results;
  }


  /**
   * Retrieves the gene name from the database based on a given label.
   * This method constructs a SQL query to fetch the synonym of a gene that matches the given label,
   * ensuring that the gene is associated with a valid data source and genome region, and that the
   * synonym is not empty. The query specifically looks for entries where the data source's BIGG ID
   * matches the REFSEQ naming convention.
   *
   * @param label The label used to query the gene name, typically a BIGG ID.
   * @return An {@link Optional} containing the gene name if found, otherwise an empty {@link Optional}.
   */
  public static Optional<String> getGeneName(String label) {
    String query = "SELECT s." + SYNONYM + "\n" + "FROM  " + DATA_SOURCE + " d, " + SYNONYM + " s, " + GENOME_REGION
      + " gr\n" + "WHERE d." + ID + " = s." + DATA_SOURCE_ID + " AND\n s." + OME_ID + " = gr." + ID + " AND\n gr."
      + BIGG_ID + " = ? AND\n d." + BIGG_ID + " LIKE " + REFSEQ_NAME + " AND s." + SYNONYM_COL + " <> ''";
    return singleParamStatement(query, label);
  }


  /**
   * Retrieves formatted gene reaction rules for a specific reaction and model from the database.
   * This method constructs a SQL query to fetch and format the gene reaction rules associated with
   * the given reaction ID and model ID. The formatting includes replacing logical operators 'or' and 'and'
   * with {@code ||} and {@code &&}, respectively, and substituting certain characters to comply with SBML standards.
   *
   * @param reactionId The ID of the reaction for which gene reaction rules are to be retrieved.
   * @param modelId The ID of the model associated with the reaction.
   * @return A list of formatted gene reaction rules as strings.
   */
  public static List<String> getGeneReactionRule(String reactionId, String modelId) {
    return getReactionRules("SELECT REPLACE(REPLACE(RTRIM(REPLACE(REPLACE(mr." + GENE_REACTION_RULE
      + ", 'or', '||'), 'and', '&&'), '.'), '.', '__SBML_DOT__'), '_AT', '__SBML_DOT__') AS " + GENE_REACTION_RULE
      + " FROM " + MODEL_REACTION + " mr, " + REACTION + " r, " + MODEL + " m WHERE r." + ID + " = mr." + REACTION_ID
      + " AND m." + ID + " = mr." + MODEL_ID + " AND mr." + GENE_REACTION_RULE + " IS NOT NULL AND  LENGTH(mr."
      + GENE_REACTION_RULE + ") > 0 AND r." + BIGG_ID + " = ? AND m." + BIGG_ID + " = ? AND mr." + GENE_REACTION_RULE
      + " <> '' ORDER BY mr." + ID, reactionId, modelId);
  }


  /**
   * Executes a provided SQL query to retrieve gene reaction rules from the database.
   * This method prepares a statement with the given query, setting the specified reactionId and modelId as parameters.
   * It then executes the query and collects the results into a list of strings.
   *
   * @param query The SQL query string that retrieves gene reaction rules, expecting two placeholders for parameters.
   * @param reactionId The ID of the reaction to be used as the first parameter in the SQL query.
   * @param modelId The ID of the model to be used as the second parameter in the SQL query.
   * @return A list of strings where each string is a gene reaction rule retrieved based on the given IDs.
   */
  public static List<String> getReactionRules(String query, String reactionId, String modelId) {
    List<String> results = new ArrayList<>();
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, reactionId);
      pStatement.setString(2, modelId);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        results.add(resultSet.getString(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return results;
  }

  
  /**
   * Retrieves the organism associated with a given BiGG model abbreviation from the database.
   * This method constructs and executes a SQL query that joins the GENOME and MODEL tables to find the organism
   * corresponding to the specified model abbreviation.
   *
   * @param abbreviation The abbreviation of the model for which the organism is to be retrieved.
   * @return An Optional containing the organism name if found, otherwise an empty Optional.
   */
  public static Optional<String> getOrganism(String abbreviation) {
    String query = "SELECT g." + ORGANISM + " FROM " + GENOME + " g, " + MODEL + " m WHERE m." + GENOME_ID + " = g."
      + ID + " AND m." + BIGG_ID + " = ?";
    return singleParamStatement(query, abbreviation);
  }


  /**
   * Retrieves a list of publications associated with a given BiGG model abbreviation from the database.
   * This method constructs and executes a SQL query that joins the PUBLICATION, PUBLICATION_MODEL, and MODEL tables
   * to find the publications related to the specified model abbreviation.
   *
   * @param abbreviation The abbreviation of the model for which the publications are to be retrieved.
   * @return A list of pairs where each pair consists of a publication type and its corresponding ID.
   */
  public static List<Pair<String, String>> getPublications(String abbreviation) {
    List<Pair<String, String>> results = new LinkedList<>();
    String query = "SELECT p." + REFERENCE_TYPE + ", p." + REFERENCE_ID + " FROM  " + PUBLICATION + " p, "
      + PUBLICATION_MODEL + " pm, " + MODEL + " m WHERE p." + ID + " = pm." + PUBLICATION_ID + " AND pm." + MODEL_ID
      + " = m." + ID + " AND m." + BIGG_ID + " = ?";
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, abbreviation);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        String key = resultSet.getString(1);
        results.add(pairOf(key.equals("pmid") ? "pubmed" : key, resultSet.getString(2)));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return results;
  }


  /**
   * Retrieves the name of a reaction based on its BiGG ID abbreviation, ensuring the name is not empty.
   * This method constructs and executes a SQL query that selects the reaction name from the REACTION table
   * where the BIGG_ID matches the specified abbreviation and the name is not an empty string.
   *
   * @param abbreviation The abbreviation of the reaction for which the name is to be retrieved.
   * @return An Optional containing the reaction name if found and not empty, otherwise an empty Optional.
   */
  public static Optional<String> getReactionName(String abbreviation) {
    String query = "SELECT " + NAME + " FROM " + REACTION + " WHERE " + BIGG_ID + " = ? AND " + NAME + " <> ''";
    return singleParamStatement(query, abbreviation);
  }

  
  /**
   * Retrieves a set of resource URLs for a given BiGG ID, optionally filtering to include only those containing 'identifiers.org'.
   * This method constructs and executes a SQL query to fetch URLs from the database based on the type of BiGG ID (reaction or component).
   * It then filters these URLs based on the 'includeAnyURI' parameter.
   *
   * @param biggId The BiGG ID object containing the abbreviation of the model component or reaction.
   * @param includeAnyURI If true, all URLs are included; if false, only URLs containing 'identifiers.org' are included.
   * @param isReaction If true, the BiGG ID is treated as a reaction; if false, it is treated as a component.
   * @return A sorted set of URLs as strings, potentially filtered by the 'identifiers.org' domain.
   */
  public static Set<String> getResources(BiGGId biggId, boolean includeAnyURI, boolean isReaction) {
    String type = isReaction ? REACTION : COMPONENT;
    Set<String> resources = new TreeSet<>();
    try {
      String query = format(
        "SELECT CONCAT(" + URL_PREFIX + ", s." + SYNONYM_COL + ") AS " + URL + " FROM {0} t, " + SYNONYM + " s, "
          + DATA_SOURCE + " d WHERE t." + ID + " = s." + OME_ID + " AND s." + DATA_SOURCE_ID + " = d." + ID + " AND "
          + URL_PREFIX + " IS NOT NULL AND {1} AND t." + BIGG_ID + " = ? {2}",
        type, getTypeQuery(isReaction), includeAnyURI ? "" : "AND " + URL_PREFIX + " LIKE '%%identifiers.org%%'");
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, biggId.getAbbreviation());
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        String result = resultSet.getString(1);
        Optional<String> url = Registry.checkResourceUrl(result);
        if (!result.contains("identifiers.org") && url.isPresent() && url.get().contains("identifiers.org")) {
          // add original resource
          resources.add(result);
        }
        // add identifiers.org resource
        url.map(resources::add);
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return resources;
  }


  /**
   * Constructs a SQL query condition based on whether the subject is a reaction or a component.
   * This method dynamically generates part of a SQL WHERE clause. If the subject is a reaction,
   * it matches the type strictly with 'REACTION'. If it's not a reaction, it matches the type
   * with either 'COMPONENT' or 'COMPARTMENTALIZED_COMPONENT'.
   *
   * @param isReaction A boolean indicating if the subject is a reaction (true) or not (false).
   * @return A string representing a SQL WHERE clause condition based on the type of subject.
   */
  private static String getTypeQuery(boolean isReaction) {
    if (isReaction) {
      return "CAST(s." + TYPE + " AS \"text\") = '" + REACTION + "'";
    }
    return "(CAST(s." + TYPE + " AS \"text\") = '" + COMPONENT + "' OR CAST(s." + TYPE + " AS \"text\") = '"
      + COMPARTMENTALIZED_COMPONENT + "')";
  }


  /**
   * Retrieves the taxonomic identifier (taxon ID) for a given model based on its abbreviation.
   * This method queries the database to find the taxon ID associated with the model's abbreviation.
   * If multiple taxon IDs are found for the same abbreviation, a severe log message is generated.
   * 
   * @param abbreviation The abbreviation of the model for which the taxon ID is being queried.
   * @return An {@link Optional} containing the taxon ID if found; otherwise, an empty {@link Optional}.
   */
  public static Optional<Integer> getTaxonId(String abbreviation) {
    Integer result = null;
    String query = "SELECT " + TAXON_ID + " FROM " + GENOME + " g, " + MODEL + " m WHERE g." + ID + " = m." + GENOME_ID
      + " AND m." + BIGG_ID + " = ? AND " + TAXON_ID + " IS NOT NULL";
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, abbreviation);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        if (result != null) {
          logger.severe(format(MESSAGES.getString("QUERY_TAXON_MULTIPLE_RESULTS"), abbreviation));
        } else {
          result = resultSet.getInt(1);
        }
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(format(MESSAGES.getString("GET_TAXON_ERROR"), abbreviation, Utils.getMessage(exc)));
    }
    return result == null ? Optional.empty() : Optional.of(result);
  }


  /**
   * Retrieves the genome accession for a given model ID from the BiGG database.
   * The accession can be used to construct URLs for accessing genomic data from various sources.
   * The URLs can be formed using the accession ID with the following patterns:
   * - https://identifiers.org/refseq:{$id}
   * - https://www.ncbi.nlm.nih.gov/nuccore/{$id}
   * - https://www.ncbi.nlm.nih.gov/assembly/{$id}
   *
   * @param id The model ID present in BiGG.
   * @return The accession string which can be appended to the base URLs mentioned above.
   *         If the query fails or no accession is found, an empty string is returned.
   */
  public static String getGenomeAccesion(String id) {
    String query = "SELECT g." + ACCESSION_VALUE + " FROM " + GENOME + " g, " + MODEL + " m WHERE m." + BIGG_ID
      + " = ? AND m." + GENOME_ID + " = g." + ID;
    String result = "";
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, id);
      ResultSet resultSet = pStatement.executeQuery();
      // There should always be exactly one entry, as the presence of a BiGG model ID is verified beforehand.
      if (resultSet.next()) {
        result = resultSet.getString(1);
      }
      pStatement.close();
      connection.close();
    } catch (SQLException e) {
      logger.warning(format(MESSAGES.getString("GENOME_ACCESSION_FAIL"), id));
    }
    // The result should be non-empty as the query is expected to always return exactly one result.
    return result;
  }


  /**
   * Retrieves a set of unique BiGG IDs from a specified table in the database.
   * This method queries the database for all unique BiGG IDs in the specified table and returns them as a set.
   * The IDs are ordered by their natural ordering in the database.
   *
   * @param table The name of the database table from which to retrieve BiGG IDs.
   * @return A Set of strings containing unique BiGG IDs from the specified table. If an SQL error occurs,
   *         the returned set will be empty.
   */
  public static Set<String> getOnce(String table) {
    Set<String> biggIds = new LinkedHashSet<>();
    try {
      Connection connection = connector.getConnection();
      String query = "SELECT " + BIGG_ID + " FROM " + table + " ORDER BY " + BIGG_ID;
      PreparedStatement pStatement = connection.prepareStatement(query);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        biggIds.add(resultSet.getString(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(format(MESSAGES.getString("BIGGID_FOR_TABLE_FAIL"), table, Utils.getMessage(exc)));
    }
    return biggIds;
  }


  /**
   * Retrieves the charge associated with a specific component in a given compartment when the model ID is unknown.
   * This method executes a SQL query to find a distinct charge value for a component based on its BiGG ID and
   * the compartment's BiGG ID. The method ensures that the charge value is not empty and returns it if it is unique.
   *
   * @param componentId The BiGG ID of the component.
   * @param compartmentId The BiGG ID of the compartment.
   * @return An {@link Optional} containing the charge if it is unique and present; otherwise, an empty {@link Optional}.
   *         If multiple unique charge values are found, a warning is logged.
   */
  public static Optional<Integer> getChargeByCompartment(String componentId, String compartmentId) {
    String query = "SELECT DISTINCT mcc." + CHARGE + " FROM " + MCC + " mcc, " + COMPARTMENTALIZED_COMPONENT + " cc, "
      + COMPONENT + " c, " + COMPARTMENT + " co WHERE c." + BIGG_ID + " = ? AND c." + ID + " = cc." + COMPONENT_ID
      + " AND co." + BIGG_ID + " = ? AND co." + ID + " = cc." + COMPARTMENT_ID + " and cc." + ID + " = mcc."
      + COMPARTMENTALIZED_COMPONENT_ID + " AND LENGTH(CAST( mcc." + CHARGE + " AS text)) > 0 ORDER BY mcc." + CHARGE;
    Set<String> results = runChargeQuery(query, componentId, compartmentId);
    if (results.size() == 1) {
      return Optional.of(Integer.parseInt(results.iterator().next()));
    } else {
      if (results.size() > 1) {
        logger.warning(format(MESSAGES.getString("CHARGE_NOT_UNIQUE_COMPARTMENT"), componentId, compartmentId));
      }
      return Optional.empty();
    }
  }


  /**
   * Executes a SQL query to retrieve distinct charge values based on the provided query string.
   * This method prepares and executes a SQL statement using the provided component ID and compartment or model ID.
   * It collects the results into a set, ensuring that only non-null and non-empty values are included.
   *
   * @param query The SQL query string to execute, expecting placeholders for componentId and compartmentOrModelId.
   * @param componentId The BiGG ID of the component, used to replace the first placeholder in the query.
   * @param compartmentOrModelId The BiGG ID of the compartment or model, used to replace the second placeholder in the query.
   * @return A Set of strings containing distinct charge values from the query results. If no valid results are found, returns an empty set.
   */
  private static Set<String> runChargeQuery(String query, String componentId, String compartmentOrModelId) {
    Set<String> results = new HashSet<>();
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, componentId);
      pStatement.setString(2, compartmentOrModelId);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        results.add(resultSet.getString(1));
      }
      pStatement.close();
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return results.stream().filter(charge -> charge != null && !charge.isEmpty()).collect(Collectors.toSet());
  }


  /**
   * Retrieves the charge for a given component and model from the database.
   * This method executes a SQL query to select distinct charge values associated with the specified component ID
   * and model ID. It ensures that the charge value is not null.
   *
   * @param componentId The BiGG ID of the component.
   * @param modelId The BiGG ID of the model.
   * @return An Optional containing the charge if exactly one distinct charge is found, otherwise an empty Optional.
   *         If multiple distinct charges are found, a warning is logged.
   */
  public static Optional<Integer> getCharge(String componentId, String modelId) {
    String query = "SELECT DISTINCT mcc." + CHARGE + "\n FROM " + COMPONENT + " c,\n" + COMPARTMENTALIZED_COMPONENT
      + " cc,\n" + MODEL + " m,\n" + MCC + " mcc\n WHERE c." + ID + " = cc." + COMPONENT_ID + " AND\n cc." + ID
      + " = mcc." + COMPARTMENTALIZED_COMPONENT_ID + " AND\n c." + BIGG_ID + " = ? AND\n m." + BIGG_ID + " = ? AND\n m."
      + ID + " = mcc." + MODEL_ID + " AND mcc." + CHARGE + " IS NOT NULL";
    Set<String> results = runChargeQuery(query, componentId, modelId);
    if (results.size() == 1) {
      return Optional.of(Integer.parseInt(results.iterator().next()));
    } else {
      if (results.size() > 1) {
        logger.warning(format(MESSAGES.getString("CHARGE_NOT_UNIQUE_MODEL"), componentId, modelId));
      }
      return Optional.empty();
    }
  }


  /**
   * Determines if a given reaction ID corresponds to a pseudoreaction in the database.
   * A pseudoreaction is typically used to represent non-biochemical data flows such as biomass accumulation,
   * demand reactions, or exchange reactions.
   *
   * @param reactionId The BiGG ID of the reaction to be checked.
   * @return true if the reaction is a pseudoreaction, false otherwise.
   */
  public static boolean isPseudoreaction(String reactionId) {
    String query = "SELECT " + PSEUDOREACTION + " FROM " + REACTION + " WHERE " + BIGG_ID + " = ?";
    Optional<String> result = singleParamStatement(query, reactionId);
    return result.isPresent() && result.get().equals("t");
  }


  /**
   * Retrieves the BiGG ID associated with a given synonym and type from the specified data source.
   * This method constructs a SQL query based on the type of biological entity (species, reaction, or gene product)
   * and executes it to fetch the corresponding BiGG ID from the database.
   *
   * @param dataSourceId The ID of the data source where the synonym is registered.
   * @param synonym The synonym used to identify the entity in the data source.
   * @param type The type of the entity, which can be species, reaction, or gene product.
   * @return An Optional containing the BiGG ID if exactly one unique ID is found, otherwise an empty Optional.
   */
  public static Optional<String> getBiggIdFromSynonym(String dataSourceId, String synonym, String type) {
    Set<String> results = new HashSet<>();
    String query;
    // Common part of the SQL query used for all types
    String sharedQuerySubstring = DATA_SOURCE + " d, " + SYNONYM + " s" + " WHERE " + "d." + BIGG_ID + " = ? AND d."
      + ID + " = s." + DATA_SOURCE_ID + " AND s." + SYNONYM_COL + " = ? AND s." + OME_ID;
    // Constructing specific query based on the type
    switch (type) {
    case TYPE_SPECIES:
      query = "SELECT " + "c." + BIGG_ID + " FROM " + COMPONENT + " c, " + sharedQuerySubstring + " = c." + ID;
      break;
    case TYPE_REACTION:
      query = "SELECT " + "r." + BIGG_ID + " FROM " + REACTION + " r, " + sharedQuerySubstring + " = r." + ID;
      break;
    case TYPE_GENE_PRODUCT:
      query = "SELECT " + "g." + LOCUS_TAG + " FROM " + GENE + " g, " + sharedQuerySubstring + " = g." + ID;
      break;
    default:
      return Optional.empty();
    }
    try {
      Connection connection = connector.getConnection();
      PreparedStatement pStatement = connection.prepareStatement(query);
      pStatement.setString(1, dataSourceId);
      pStatement.setString(2, synonym);
      ResultSet resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        results.add(resultSet.getString(1));
      }
      connection.close();
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    // Filtering and returning the result
    results = results.stream().filter(biggId -> biggId != null && !biggId.isEmpty()).collect(Collectors.toSet());
    if (results.size() == 1) {
      return Optional.of(results.iterator().next());
    } else {
      return Optional.empty();
    }
  }


  /**
   * Represents a reaction from an external data source mapped to the BiGG database, including its compartment details.
   * "Foreign" in this context refers to the origin of the reaction data from a source outside of the primary BiGG database schema,
   * typically involving cross-referencing with external databases or data sources.
   */
  public static class ForeignReaction {
    public final String reactionId;       // The BiGG ID of the reaction.
    public final String compartmentId;    // The BiGG ID of the compartment.
    public final String compartmentName;  // The name of the compartment.

    /**
     * Constructs a new ForeignReaction instance.
     *
     * @param reactionId The BiGG ID of the reaction.
     * @param compartmentId The BiGG ID of the compartment.
     * @param compartmentName The name of the compartment.
     */
    public ForeignReaction(String reactionId, String compartmentId, String compartmentName) {
      this.reactionId = reactionId;
      this.compartmentId = compartmentId;
      this.compartmentName = compartmentName;
    }
  }

  /**
   * Retrieves a collection of ForeignReaction objects for a given synonym and data source ID.
   * This method queries the database to find reactions and their associated compartment details
   * based on the provided synonym and data source ID. The term "foreign" indicates that the reactions
   * are identified using external data sources, which are then mapped to corresponding entities in the BiGG database.
   *
   * @param dataSourceId The ID of the data source where the synonym is registered.
   * @param synonym The synonym used to identify the reaction in the data source.
   * @return A collection of ForeignReaction objects containing the reaction and compartment details.
   */
  public static Collection<ForeignReaction> getBiggIdsForReactionForeignId(String dataSourceId, String synonym) {
    Set<ForeignReaction> results = new HashSet<>();

    // SQL query to fetch reaction and compartment details
    var query = "SELECT R.BIGG_ID AS REACTION_BIGG_ID, "
            + "C.BIGG_ID AS COMPARTMENT_BIGG_ID, "
            + "C.NAME AS COMPARTMENT_NAME "
            + "FROM REACTION R "
            + "left join REACTION_MATRIX RM "
            + "on RM.REACTION_ID = R.ID "
            + "left join COMPARTMENTALIZED_COMPONENT CC "
            + "on RM.COMPARTMENTALIZED_COMPONENT_ID = CC.ID "
            + "left join COMPARTMENT C "
            + "on CC.COMPARTMENT_ID = C.ID "
            + "join synonym s "
            + "on synonym = ? and r.id = s.ome_id "
            + "join data_source d "
            + "on s.data_source_id = d.id and d.bigg_id = ?";

    try (var connection = connector.getConnection()) {
      var pStatement = connection.prepareStatement(query);
      pStatement.setString(1, synonym);
      pStatement.setString(2, dataSourceId);
      var resultSet = pStatement.executeQuery();
      while (resultSet.next()) {
        var reactionBiggId = resultSet.getString(1);
        var compartmentBiggId = resultSet.getString(2);
        var compartmentName = resultSet.getString(3);
        var r = new ForeignReaction(reactionBiggId, compartmentBiggId, compartmentName);
        results.add(r);
      }
    } catch (SQLException exc) {
      logger.warning(Utils.getMessage(exc));
    }
    return results;
  }
}
