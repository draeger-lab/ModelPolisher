package edu.ucsd.sbrg.resolver.identifiersorg;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.ucsd.sbrg.resolver.identifiersorg.mapping.RawIdentifiersOrgRegistry;

/**
 * The {@code IdentifiersOrgRegistryParser} class is a singleton that provides functionality to parse the MIRIAM registry
 * from a JSON file and convert it into a {@code Miriam} object. This class ensures that only one instance
 * of the parser is created and used throughout the application.
 */
public class IdentifiersOrgRegistryParser {

  private static final Logger logger = Logger.getLogger(IdentifiersOrgRegistryParser.class.getName());

  public RawIdentifiersOrgRegistry parse(InputStream registry) throws IOException {
    logger.fine("Parsing MIRIAM registry");

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    return mapper.readValue(registry, RawIdentifiersOrgRegistry.class);
  }
}
