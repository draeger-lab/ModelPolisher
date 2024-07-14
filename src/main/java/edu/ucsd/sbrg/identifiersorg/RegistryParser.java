package edu.ucsd.sbrg.identifiersorg;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsd.sbrg.identifiersorg.models.IdentifiersOrgRegistry;
import edu.ucsd.sbrg.identifiersorg.models.Namespace;
import edu.ucsd.sbrg.identifiersorg.models.Root;

/**
 * The {@code RegistryParser} class is a singleton that provides functionality to parse the MIRIAM registry
 * from a JSON file and convert it into a {@code Miriam} object. This class ensures that only one instance
 * of the parser is created and used throughout the application.
 */
public class RegistryParser {

  private static final Logger logger = Logger.getLogger(RegistryParser.class.getName());

  /**
   * Singleton instance of {@code RegistryParser}.
   */
  private static RegistryParser parser;

  /**
   * InputStream to read the MIRIAM registry JSON file.
   */
  private static InputStream registry;

  /**
   * Private constructor to prevent instantiation from outside this class.
   * Initializes the InputStream for the MIRIAM registry JSON file.
   */
  private RegistryParser() {
    super();
    registry = RegistryParser.class.getResourceAsStream("IdentifiersOrg-Registry.json");
  }

  /**
   * Provides the singleton instance of {@code RegistryParser}.
   * If the instance is not already created, it initializes a new one.
   *
   * @return The singleton instance of {@code RegistryParser}.
   */
  public static RegistryParser getInstance() {
    if (parser == null) {
      parser = new RegistryParser();
    }
    return parser;
  }

  /**
   * Parses the MIRIAM registry JSON file into a {@code Miriam} object.
   * It reads the JSON structure, extracts namespaces, and maps them by their prefixes.
   *
   * @return A {@code Miriam} object initialized with the parsed namespaces.
   * @throws IOException If there is an error reading the JSON file.
   */
  IdentifiersOrgRegistry parse() throws IOException {
    logger.fine("Parsing MIRIAM registry");
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Root root =  mapper.readValue(registry, Root.class);
    List<Namespace> namespaces = root.getPayload().get("namespaces");
    HashMap<String, Namespace> prefixIndexedNamespaces = new HashMap<>();
    namespaces.forEach(x -> prefixIndexedNamespaces.put(x.getPrefix(), x));
    return IdentifiersOrgRegistry.initFrom(prefixIndexedNamespaces);
  }
}
