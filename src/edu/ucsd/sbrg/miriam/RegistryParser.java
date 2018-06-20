package edu.ucsd.sbrg.miriam;

import edu.ucsd.sbrg.miriam.xjc.Miriam;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.net.URL;

public class RegistryParser {

  private static RegistryParser parser;
  private static URL registryLocation;


  public static RegistryParser getInstance() {
    if (parser == null) {
      parser = new RegistryParser();
    }
    return parser;
  }


  Miriam parse() throws JAXBException {
    // stax/sax parser might be more efficient, as we only need a subset of the data
    // we parse the whole tree for now
    JAXBContext ctx = JAXBContext.newInstance(Miriam.class);
    Unmarshaller unmarshaller = ctx.createUnmarshaller();
    return (Miriam) unmarshaller.unmarshal(registryLocation);
  }


  private RegistryParser() {
    super();
    registryLocation = RegistryParser.class.getResource("IdentifiersOrg-Registry.xml");
  }
}
