package edu.ucsd.sbrg.miriam;

import edu.ucsd.sbrg.miriam.xjc.Miriam;
import edu.ucsd.sbrg.miriam.xjc.Uri;
import edu.ucsd.sbrg.miriam.xjc.Uris;

import javax.xml.bind.JAXBException;
import java.util.HashMap;

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
