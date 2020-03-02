package edu.ucsd.sbrg.miriam;

import java.io.IOException;

import edu.ucsd.sbrg.miriam.models.Miriam;

public class RegistryProvider {

  private static RegistryProvider provider;
  private static Miriam miriam;

  /**
   * 
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
   * @return
   */
  public static RegistryProvider getInstance() {
    if (provider == null) {
      provider = new RegistryProvider();
    }
    return provider;
  }


  /**
   * 
   */
  public static void close() {
    provider = null;
    miriam = null;
  }


  /**
   * @return
   */
  public Miriam getMiriam() {
    return miriam;
  }
}
