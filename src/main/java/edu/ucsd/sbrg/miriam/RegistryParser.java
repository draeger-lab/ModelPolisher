package edu.ucsd.sbrg.miriam;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsd.sbrg.miriam.models.Miriam;
import edu.ucsd.sbrg.miriam.models.Namespace;
import edu.ucsd.sbrg.miriam.models.Root;

public class RegistryParser {

  private static final Logger logger = Logger.getLogger(RegistryParser.class.getName());
  private static RegistryParser parser;
  private static InputStream registry;

  private RegistryParser() {
    super();
    registry = RegistryParser.class.getResourceAsStream("IdentifiersOrg-Registry.json");
  }


  public static RegistryParser getInstance() {
    if (parser == null) {
      parser = new RegistryParser();
    }
    return parser;
  }


  Miriam parse() throws IOException {
    logger.fine("Parsing MIRIAM registry");
    ObjectMapper mapper = new ObjectMapper();
    Root root =  mapper.readValue(registry, Root.class);
    List<Namespace> namespaces = root.getPayload().get("namespaces");
    HashMap<String, Namespace> prefixIndexedNamespaces = new HashMap<>();
    namespaces.forEach(x -> prefixIndexedNamespaces.put(x.getPrefix(), x));
    return Miriam.initFrom(prefixIndexedNamespaces);
  }
}
