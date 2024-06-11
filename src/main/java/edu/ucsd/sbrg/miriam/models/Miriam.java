package edu.ucsd.sbrg.miriam.models;

import java.util.Map;

/**
 * The {@code Miriam} class is a singleton that manages a collection of namespaces.
 * It provides a centralized access point to namespaces mapped by their prefixes.
 * This class ensures that only one instance of itself is created and used throughout the application.
 */
public class Miriam {

  /**
   * The singleton instance of {@code Miriam}.
   */
  private static final Miriam miriam = new Miriam();

  /**
   * A map of namespace prefixes to their corresponding {@code Namespace} objects.
   */
  private static Map<String, Namespace> namespaces;

  private Miriam() {
    super();
  }

  /**
   * Initializes the singleton instance with a map of namespaces.
   * 
   * @param namespaces A map of namespace prefixes to their corresponding {@code Namespace} objects.
   * @return The singleton instance of {@code Miriam}.
   */
  public static Miriam initFrom(Map<String, Namespace> namespaces) {
    Miriam.namespaces = namespaces;
    return miriam;
  }

  /**
   * Provides the singleton instance of {@code Miriam}.
   * 
   * @return The singleton instance of {@code Miriam}.
   * @throws IllegalStateException if the singleton instance has not been initialized.
   */
  public Miriam getInstance() {
    if (miriam == null) {
      throw new IllegalStateException("Instance not initialized.");
    }
    return miriam;
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
