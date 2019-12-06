package edu.ucsd.sbrg.miriam;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
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
  private static URL registryLocation;

  private RegistryParser() {
    super();
    registryLocation = RegistryParser.class.getResource("IdentifiersOrg-Registry.json");
  }


  public static RegistryParser getInstance() {
    if (parser == null) {
      parser = new RegistryParser();
    }
    return parser;
  }


  Miriam parse() throws URISyntaxException, IOException {
    logger.fine("Parsing MIRIAM registry");
    File json = Paths.get(registryLocation.toURI()).toFile();
    ObjectMapper mapper = new ObjectMapper();
    Root root = mapper.readValue(json, Root.class);
    List<Namespace> namespaces = root.getPayload().get("namespaces");
    HashMap<String, Namespace> prefixIndexedNamespaces = new HashMap<>();
    namespaces.forEach(x -> prefixIndexedNamespaces.put(x.getPrefix(), x));
    return Miriam.initFrom(prefixIndexedNamespaces);
  }
}
