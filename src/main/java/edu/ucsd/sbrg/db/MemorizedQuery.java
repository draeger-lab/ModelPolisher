package edu.ucsd.sbrg.db;

import edu.ucsd.sbrg.db.bigg.BiGGDB;

import java.util.LinkedHashSet;
import java.util.Set;

public class MemorizedQuery {

  private static Set<String> BiGGDBCompartments = new LinkedHashSet<>();
  private static Set<String> BiGGDBDataSources = new LinkedHashSet<>();
  private static Set<String> BiGGDBMetabolites = new LinkedHashSet<>();
  private static Set<String> BiGGDBModels = new LinkedHashSet<>();
  private static Set<String> BiGGDBReactions = new LinkedHashSet<>();

  /**
   * @param id
   * @return
   */
  public static boolean isCompartment(String id) {
    if (BiGGDBCompartments.isEmpty()) {
      BiGGDBCompartments = BiGGDB.getOnce("compartment");
    }
    if (id.startsWith("C_")) {
      id = id.substring(2);
    }
    return BiGGDBCompartments.contains(id);
  }


  /**
   * @param id
   * @return
   */
  public static boolean isDataSource(String id) {
    if (BiGGDBDataSources.isEmpty()) {
      BiGGDBDataSources = BiGGDB.getOnce("data_source");
    }
    return BiGGDBDataSources.contains(id);
  }


  /**
   * @param id
   * @return
   */
  public static boolean isMetabolite(String id) {
    if (BiGGDBMetabolites.isEmpty()) {
      BiGGDBMetabolites = BiGGDB.getOnce("component");
    }
    if (id.startsWith("M_")) {
      id = id.substring(2);
    }
    return BiGGDBMetabolites.contains(id);
  }


  /**
   * @param id
   * @return
   */
  public static boolean isModel(String id) {
    if (BiGGDBModels.isEmpty()) {
      BiGGDBModels = BiGGDB.getOnce("model");
    }
    return BiGGDBModels.contains(id);
  }


  /**
   * @param id
   * @return
   */
  public static boolean isReaction(String id) {
    if (BiGGDBReactions.isEmpty()) {
      BiGGDBReactions = BiGGDB.getOnce("reaction");
    }
    if (id.startsWith("R_")) {
      id = id.substring(2);
    }
    return BiGGDBReactions.contains(id);
  }
}
