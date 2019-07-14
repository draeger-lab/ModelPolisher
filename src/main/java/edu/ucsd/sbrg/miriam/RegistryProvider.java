package main.java.edu.ucsd.sbrg.miriam;

import javax.xml.bind.JAXBException;

import main.java.edu.ucsd.sbrg.miriam.xjc.Miriam;

public class RegistryProvider {

  private static RegistryProvider provider;
  private static Miriam miriam;


  private RegistryProvider() {
    super();
    try {
      miriam = RegistryParser.getInstance().parse();
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }


  public static RegistryProvider getInstance() {
    if (provider == null) {
      provider = new RegistryProvider();
    }
    return provider;
  }


  public static void close() {
    provider = null;
    miriam = null;
  }


  public Miriam getMiriam() {
    return miriam;
  }
}
