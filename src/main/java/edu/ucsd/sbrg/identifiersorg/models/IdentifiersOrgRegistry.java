package edu.ucsd.sbrg.identifiersorg.models;

import java.util.Map;

/**
 * The {@code IdentifiersOrgRegistry} class is a singleton that manages a collection of namespaces.
 * It provides a centralized access point to namespaces mapped by their prefixes.
 * This class ensures that only one instance of itself is created and used throughout the application.
 */
public class IdentifiersOrgRegistry {

  private static final IdentifiersOrgRegistry IDENTIFIERS_ORG_REGISTRY = new IdentifiersOrgRegistry();

  /**
   * A map of namespace prefixes to their corresponding {@code Namespace} objects.
   */
  private static Map<String, Namespace> namespaces;

  private IdentifiersOrgRegistry() {
    super();
  }

  /**
   * Initializes the singleton instance with a map of namespaces.
   * 
   * @param namespaces A map of namespace prefixes to their corresponding {@code Namespace} objects.
   * @return The singleton instance of {@code IdentifiersOrgRegistry}.
   */
  public static IdentifiersOrgRegistry initFrom(Map<String, Namespace> namespaces) {
    IdentifiersOrgRegistry.namespaces = namespaces;
    return IDENTIFIERS_ORG_REGISTRY;
  }

  /**
   * Retrieves the map of namespaces.
   * 
   * @return A map of namespace prefixes to their corresponding {@code Namespace} objects.
   */
  public Map<String, Namespace> getNamespaces(){
    return namespaces;
  }
}
