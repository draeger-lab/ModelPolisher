package edu.ucsd.sbrg.identifiersorg;

import java.io.IOException;

import edu.ucsd.sbrg.identifiersorg.models.IdentifiersOrgRegistry;

public class RegistryProvider {

  private static RegistryProvider provider;
  private static IdentifiersOrgRegistry identifiersOrgRegistry;

  /**
   * Private constructor for the singleton RegistryProvider class.
   * It initializes the Miriam instance by parsing data using the RegistryParser.
   * If an IOException occurs during parsing, it prints the stack trace.
   */
  private RegistryProvider() {
    super();
    try {
      identifiersOrgRegistry = RegistryParser.getInstance().parse();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Provides access to the singleton instance of RegistryProvider.
   * If the instance does not exist, it creates a new one.
   * 
   * @return The singleton instance of RegistryProvider.
   */
  public static RegistryProvider getInstance() {
    if (provider == null) {
      provider = new RegistryProvider();
    }
    return provider;
  }

  /**
   * Resets the singleton instance of RegistryProvider and its associated Miriam instance to null.
   * This method can be used to release resources or reinitialize the instances.
   */
  public static void close() {
    provider = null;
    identifiersOrgRegistry = null;
  }

  /**
   * Retrieves the current Miriam instance.
   * 
   * @return The current Miriam instance.
   */
  public IdentifiersOrgRegistry getMiriam() {
    return identifiersOrgRegistry;
  }
}
