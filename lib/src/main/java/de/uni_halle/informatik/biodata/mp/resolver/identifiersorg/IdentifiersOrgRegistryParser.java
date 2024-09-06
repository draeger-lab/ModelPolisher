package de.uni_halle.informatik.biodata.mp.resolver.identifiersorg;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.mapping.RawIdentifiersOrgRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code IdentifiersOrgRegistryParser} class is a singleton that provides functionality to parse the MIRIAM registry
 * from a JSON file and convert it into a {@code Miriam} object. This class ensures that only one instance
 * of the parser is created and used throughout the application.
 */
public class IdentifiersOrgRegistryParser {

  private static final Logger logger = LoggerFactory.getLogger(IdentifiersOrgRegistryParser.class);

  public RawIdentifiersOrgRegistry parse(InputStream registry) throws IOException {
    logger.trace("Parsing MIRIAM registry");

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    return mapper.readValue(registry, RawIdentifiersOrgRegistry.class);
  }
}
