package edu.ucsd.sbrg.miriam;

import java.io.IOException;

import edu.ucsd.sbrg.miriam.models.Miriam;

public class RegistryProvider {

  private static RegistryProvider provider;
  private static Miriam miriam;

  /**
   * Private constructor for the singleton RegistryProvider class.
   * It initializes the Miriam instance by parsing data using the RegistryParser.
   * If an IOException occurs during parsing, it prints the stack trace.
   */
  private RegistryProvider() {
    super();
    try {
      miriam = RegistryParser.getInstance().parse();
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
    miriam = null;
  }

  /**
   * Retrieves the current Miriam instance.
   * 
   * @return The current Miriam instance.
   */
  public Miriam getMiriam() {
    return miriam;
  }
}
