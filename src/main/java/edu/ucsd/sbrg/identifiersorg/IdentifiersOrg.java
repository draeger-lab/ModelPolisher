package edu.ucsd.sbrg.identifiersorg;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.identifiersorg.models.IdentifiersOrgRegistry;

/**
 * The {@code IdentifiersOrg} class serves as a central hub for managing and processing identifiers related to the MIRIAM registry.
 * MIRIAM is a standard for annotating computational models in biology with machine-readable information. 
 * 
 * This class provides static methods and utilities to handle, validate,
 * and correct resource URLs based on the MIRIAM standards. It ensures that identifiers and URLs conform to recognized formats and corrects common errors in identifiers from various
 * biological databases. The class also initializes necessary resources and configurations at the start through a static block.
 */
public class IdentifiersOrg {

  /**
   * A {@link Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(IdentifiersOrg.class.getName());
  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /*
   * Static initializer block for the IdentifiersOrg class.
   * This block is executed once when the Registry class is loaded.
   * It performs the following operations:
   * 1. Retrieves an instance of Miriam from the RegistryProvider.
   * 2. Converts the namespaces fetched from Miriam into a list of CompactEntry objects.
   *    This conversion only retains essential information needed for the ModelPolisher, optimizing memory usage.
   * 3. Initializes the Entries class with this list of CompactEntry objects.
   * 4. Closes the RegistryProvider to free up resources.
   */
  static {
    // Fetch the Miriam instance from the RegistryProvider
    IdentifiersOrgRegistry identifiersOrgRegistry = RegistryProvider.getInstance().getMiriam();
    // Convert the namespaces to CompactEntries and initialize Entries
    Entries.initFromList(
      identifiersOrgRegistry.getNamespaces().values().parallelStream().map(CompactEntry::fromNamespace).collect(Collectors.toList()));
    // Close the RegistryProvider to release resources
    RegistryProvider.close();
  }

  /**
   * Checks and processes a given resource URL to ensure it conforms to expected formats and corrections.
   * This method handles specific cases such as URLs containing "omim", "ncbigi", and "reactome".
   * It also processes general identifiers.org URLs and other alternative formats.
   *
   * @param resource The URL to be checked and potentially modified.
   * @return An {@link Optional} containing the processed URL if valid, or empty if the URL should be skipped.
   */
  public static Optional<String> checkResourceUrl(String resource) {
    // Remove trailing whitespaces from the URL
    resource = resource.stripTrailing();
    
    // Check if the URL starts with the OMIM prefix which is known to be invalid
    if (resource.startsWith("https://identifiers.org/omim")) {
      // OMIM is present in BiGGDB, but is not valid, skip
      return Optional.empty();
    }
    
    // Handle URLs containing the NCBI GI identifier which needs prefix correction
    if (resource.contains("ncbigi")) {
      return Optional.of(handleNCBIGI(resource));
    }
    
    // Correct the Reactome ID if necessary
    if (resource.contains("reactome")) {
      resource = fixReactomeId(resource);
    }
    
    // Compile a pattern to match identifiers.org URLs
    Pattern identifiersURL = Pattern.compile("(?:https?://)?identifiers.org/(?:(?<provider>.*?)/)?(?<id>.*)");
    Matcher urlMatcher = identifiersURL.matcher(resource);
    
    // Check if the URL matches the identifiers.org pattern and handle accordingly
    if (urlMatcher.matches()) {
      resource = handleIdentifiersURL(resource, urlMatcher);
    } else {
      // Handle alternative URL formats that do not match identifiers.org pattern
      resource = handleAlternativeURL(resource);
    }
    
    // Return the processed URL if it is not null, otherwise return an empty Optional
    if (resource != null) {
      return Optional.of(resource);
    } else {
      return Optional.empty();
    }
  }


  /**
   * Processes a resource URL to correct the NCBI GI identifier by ensuring it is properly prefixed.
   * If the identifier is found to start with "gi:", it is corrected to "GI:". If the identifier lacks
   * any prefix, "GI:" is prepended to it.
   *
   * @param resource The URL containing the NCBI GI identifier.
   * @return The URL with the corrected NCBI GI identifier.
   */
  private static String handleNCBIGI(String resource) {
    String[] split = resource.split("/");
    String id = split[split.length - 1];
    if (!id.startsWith("GI:")) {
      if (id.startsWith("gi:")) {
        resource = replace(resource, id, id.replaceAll("gi:", "GI:"));
      } else {
        resource = replace(resource, id, "GI:" + id);
      }
    }
    return resource;
  }


  /**
   * Corrects the Reactome ID in the given resource URL if it follows the pattern "R-ALL-REACT_".
   * This method checks if the resource contains the specified pattern and attempts to correct it by
   * ensuring it starts with "R-ALL-" followed by digits. If the pattern is not found or if the ID
   * after "R-ALL-REACT_" is missing, the original resource URL is returned unchanged.
   *
   * @param resource The URL containing the Reactome ID to be corrected.
   * @return The URL with the corrected Reactome ID, or the original URL if no correction was needed.
   */
  private static String fixReactomeId(String resource) {
    String identifier = "";
    if (resource.contains("R-ALL-REACT_")) {
      String[] splits = resource.split("R-ALL-REACT_");
      if(splits.length != 2){
        // If the ID is missing after "R-ALL-REACT_", return the original resource
        return resource;
      }
      identifier = splits[1];
      if (Character.isDigit(identifier.charAt(0))) {
        // Ensure the identifier starts with "R-ALL-" followed by the digits
        identifier = "R-ALL-" + identifier;
      }
      // Replace the faulty part of the URL with the corrected identifier
      return resource.replaceAll("R-ALL-REACT_.*$", identifier);
    }
    // Return the original resource if it does not contain the "R-ALL-REACT_" pattern
    return resource;
  }


  /**
   * Processes an identifiers.org URL to ensure it conforms to the expected format and corrects common errors in the
   * provider code or identifier. This method uses a {@link Matcher} to extract the provider and identifier from the URL.
   * It then validates and possibly corrects the provider code, checks the identifier against a known pattern for the provider,
   * and reconstructs the URL if necessary.
   *
   * @param resource The identifiers.org URL to be checked and corrected.
   * @param urlMatcher A {@link Matcher} used to extract the 'provider' and 'id' from the URL.
   * @return A corrected identifiers.org URL if possible, the original URL if the provider's collection is unknown,
   *         or {@code null} if the identifier cannot be corrected.
   */
  public static String handleIdentifiersURL(String resource, Matcher urlMatcher) {
    // Extract provider and identifier using the Matcher
    String provider = urlMatcher.group("provider");
    provider = provider == null ? "" : provider;
    String identifier = urlMatcher.group("id");

    // Normalize provider code to lowercase if it is in uppercase
    if (provider.matches("[A-Z]+")) {
      provider = provider.toLowerCase();
    }

    // Singleton instance of Entries to access collections and providers
    Entries entries = Entries.getInstance();

    // Attempt to find a matching provider if the extracted one is empty
    if (provider.isEmpty()) {
      Node collection = entries.getCollection(resource);
      if (collection != null) {
        provider = entries.getProviderForCollection(collection.getName());
      }
    }

    // Correct cases where provider and collection names might have been confused
    if (entries.getCollectionForProvider(provider).isEmpty() && !entries.getProviderForCollection(provider).isEmpty()) {
      provider = entries.getProviderForCollection(provider);
    } else if (entries.getCollectionForProvider(provider).isEmpty()) {
      logger.severe(format(MESSAGES.getString("UNCAUGHT_URI"), resource));
      return resource;
    }

    // Reconstruct the URL with the possibly corrected provider
    resource = createURI(provider, identifier);
    String collection = entries.getCollectionForProvider(provider);
    String regexp = entries.getPattern(collection);

    // Validate the identifier against the expected pattern
    boolean correct = checkPattern(identifier, regexp);
    String report_resource = resource;
    if (!correct) {
      logger.info(format(MESSAGES.getString("PATTERN_MISMATCH_INFO"), identifier, regexp, collection));
      resource = fixResource(resource, identifier);
    }

    // Log and handle cases where the URL could not be corrected
    if (resource == null) {
      logger.warning(format(MESSAGES.getString("CORRECTION_FAILED_DROP"), report_resource, collection));
    } else {
      logger.finer(format("Added resource {0}", resource));
    }

    return resource;
  }


  /**
   * Attempts to convert a non-identifiers.org URL into a corresponding identifiers.org URL by extracting
   * the identifier from the original URL and matching it to a known collection in the MIRIAM registry.
   * If a matching collection is found and an identifier is successfully extracted, the method constructs
   * and returns an identifiers.org URL using the provider associated with the collection. If no matching
   * collection is found or the identifier cannot be extracted, the original URL is returned.
   *
   * @param resource The original non-identifiers.org URL to be converted.
   * @return A valid identifiers.org URL if possible, otherwise the original URL.
   */
  public static String handleAlternativeURL(String resource) {
    Entries entries = Entries.getInstance();
    Node collection = entries.getCollection(resource);
    if (collection == null) {
      // No corresponding collection found in the registry, return the original URL
      return resource;
    }
    Optional<String> identifier = collection.extractId(resource);
    if (identifier.isPresent()) {
      // A valid identifier was extracted, construct the identifiers.org URL
      String provider = entries.getProviderForCollection(collection.getName());
      resource = createURI(provider, identifier);
    }
    return resource;
  }


  public static String createURI(String provider, BiGGId id) {
    return createURI(provider, id.getAbbreviation());
  }


  public static String createURI(String provider, Object id) {
    return createURI(provider, id.toString());
  }


  public static String createURI(String provider, String id) {
    return "https://identifiers.org/" + provider + "/" + id;
  }


  public static String createShortURI(Object id) {
    return "https://identifiers.org/" + id.toString();
  }


  /**
   * Attempts to correct a given resource URL based on its identifier. This method handles various cases
   * specific to different databases like ChEBI, EC-Code, GO, HMDB, KEGG, Reactome, RefSeq, and Rhea.
   * If the identifier is empty or if no specific conditions are met for the databases, the method returns null.
   * Otherwise, it returns the corrected resource URL.
   *
   * @param resource The original resource URL that may need correction.
   * @param identifier The identifier that may dictate the specific corrections needed.
   * @return The corrected resource URL, or null if no correction was possible.
   */
  private static String fixResource(String resource, String identifier) {
    if(identifier.isEmpty()){
      // this is invalid
      return null;
    }
    if (resource.contains("chebi")) {
      resource = fixChEBI(resource, identifier);
    } else if (resource.contains("ec-code")) {
      resource = fixECCode(resource, identifier);
    } else if (resource.contains("go") && !resource.contains("goa")) {
      resource = fixGO(resource, identifier);
    } else if (resource.contains("hmdb") && identifier.startsWith("/")) {
      resource = replace(resource, identifier, identifier.substring(1));
      logger.severe(format("Changed identifier {0} to {1}", identifier, identifier.substring(1)));
    } else if (resource.contains("kegg")) {
      // We can correct the kegg collection
      resource = fixKEGGCollection(resource, identifier);
    } else if (resource.contains("reactome")) {
      String resource_old = resource;
      resource = fixReactome(resource, identifier);
      logger.info(format(MESSAGES.getString("CHANGED_REACTOME"), resource_old, resource));
    } else if (resource.contains("refseq") && resource.contains("WP_")) {
      resource = replace(resource, "refseq", "ncbiprotein");
      logger.info(
        format("Ids starting with 'WP_' seem to belong to 'ncbiprotein', not 'refseq'. Changed accordingly for {0}",
          resource));
    } else if (resource.contains("rhea") && resource.contains("#")) {
      // remove last part, it's invalid either way, even though it gets resolved due to misinterpretation as non
      // existing anchor
      resource = resource.split("#")[0];
      logger.info(format(MESSAGES.getString("CHANGED_RHEA"), resource));
    } else {
      resource = null;
    }
    return resource;
  }


  /**
   * Adjusts the ChEBI identifier in the resource string by ensuring it is correctly prefixed with "CHEBI:".
   * If the identifier consists solely of digits, it is prefixed with "CHEBI:" and the resource string is updated.
   * This method logs the addition of the prefix.
   *
   * @param resource The original resource string that may contain the ChEBI identifier.
   * @param identifier The ChEBI identifier that may need to be prefixed.
   * @return The updated resource string with the correctly prefixed ChEBI identifier, or the original resource if no change was needed.
   */
  private static String fixChEBI(String resource, String identifier) {
    if (Pattern.compile("\\d+").matcher(identifier).matches()) {
      logger.info(MESSAGES.getString("ADD_PREFIX_CHEBI"));
      resource = replace(resource, identifier, "CHEBI:" + identifier);
    }
    return resource;
  }


  public static String replace(String resource, String pattern, String replacement) {
    return resource.replaceAll(pattern, replacement);
  }


  /**
   * Adjusts an EC (Enzyme Commission) code in the resource string by ensuring it has the correct number of segments.
   * EC codes should have four parts separated by three dots (e.g., 1.2.3.4). If the provided identifier has fewer than
   * three dots, this method appends the necessary number of ".-" to make up the difference.
   *
   * @param resource The original resource string that may contain the EC code.
   * @param identifier The EC code that may be incomplete.
   * @return The updated resource string with the corrected EC code, or null if the identifier has no dots.
   */
  private static String fixECCode(String resource, String identifier) {
    int missingDots = identifier.length() - identifier.replace(".", "").length();
    if (missingDots < 1) {
      logger.warning(format(MESSAGES.getString("EC_CHANGE_FAILED"), identifier));
      return null;
    }
    return replace(resource, identifier, identifier + ".-".repeat(Math.max(0, 3 - missingDots)));
  }

  /**
   * Ensures that the given identifier for a Gene Ontology (GO) term is correctly prefixed with "GO:" if it is not already.
   * If the identifier lacks the prefix, it is added, and the resource string is updated to include this prefixed identifier.
   * 
   * @param resource The original resource string that may contain the unprefixed identifier.
   * @param identifier The GO identifier that may or may not start with "GO:".
   * @return The updated resource string with the identifier correctly prefixed, or the original resource if no change was needed.
   */
  private static String fixGO(String resource, String identifier) {
    if (!identifier.toLowerCase().startsWith("go:")) {
      logger.info(MESSAGES.getString("ADD_PREFIX_GO"));
      return replace(resource, identifier, "GO:" + identifier);
    }
    return resource;
  }


  /**
   * Adjusts the KEGG collection type in the resource string based on the identifier provided.
   * This method first normalizes the identifier by capitalizing the first letter. It then checks
   * the starting character of the identifier to determine the specific KEGG collection type.
   * If the identifier starts with 'D', it is associated with "kegg.drug", and if it starts with 'G',
   * it is associated with "kegg.glycan". The resource string is updated to reflect the correct
   * collection type.
   *
   * @param resource The original resource string that may contain an incorrect KEGG collection type.
   * @param identifier The identifier that determines the correct KEGG collection type.
   * @return The updated resource string with the correct KEGG collection type.
   */
  private static String fixKEGGCollection(String resource, String identifier) {
    // Correct case mismatch in identifiers
    char first = identifier.charAt(0);
    identifier = Character.toUpperCase(first) + identifier.substring(1);
    if (identifier.startsWith("D")) {
      logger.info(MESSAGES.getString("CHANGE_KEGG_DRUG"));
      resource = replace(resource, "kegg.compound", "kegg.drug");
    } else if (identifier.startsWith("G")) {
      logger.info(MESSAGES.getString("CHANGE_KEGG_GLYCAN"));
      resource = replace(resource, "kegg.compound", "kegg.glycan");
    }
    return resource;
  }


  /**
   * Adjusts and validates a Reactome identifier, then constructs a URI if the identifier is valid.
   * <p>
   * This method processes the given identifier to ensure it conforms to expected Reactome formatting.
   * If the identifier starts with "R-ALL-REACT_", it strips this prefix and uses the remaining part.
   * If the identifier starts with a digit after any initial processing, it prepends "R-ALL-" to it.
   * Finally, it checks if the processed identifier matches the Reactome pattern. If it does, it creates
   * and returns a URI using the 'reactome' namespace; otherwise, it returns null.
   *
   * @param resource The original resource string (not directly used in the current implementation).
   * @param identifier The identifier that needs to be processed and validated.
   * @return A string representing the URI if the identifier is valid, otherwise null.
   */
  private static String fixReactome(String resource, String identifier) {
    if (identifier.startsWith("R-ALL-REACT_")) {
      identifier = identifier.split("_")[1];
    }
    if (Character.isDigit(identifier.charAt(0))) {
      identifier = "R-ALL-" + identifier;
    }
    if (checkPattern(identifier, Entries.getInstance().getPattern("Reactome"))) {
      return createURI("reactome", identifier);
    } else {
      return null;
    }
  }


  /**
   * Extracts the provider code and identifier from a valid identifiers.org URL.
   * This method parses the URL to separate the namespace prefix and the specific identifier.
   * It uses a regular expression to capture these parts from the URL.
   *
   * @param url A valid identifiers.org URL from which the provider code and identifier are to be extracted.
   * @return A list containing the provider code and identifier, if the URL is valid and matches the expected format.
   *         The list will contain the provider code as the first element and the identifier as the second element.
   *         If the URL does not match the expected format, the returned list will be empty.
   */
  public static List<String> getPartsFromIdentifiersURI(String url) {
    List<String> parts = new ArrayList<>();
    Matcher identifiersURL =
      Pattern.compile("(https?://)?(www\\.)?identifiers\\.org/(?<prefix>.*?)/(?<id>.*)").matcher(url);
    if (identifiersURL.matches()) {
      String prefix = identifiersURL.group("prefix");
      String id = identifiersURL.group("id");
      if (prefix != null) {
        parts.add(prefix);
      }
      if (id != null) {
        parts.add(id);
      }
    }
    return parts;
  }


  public static Boolean checkPattern(String id, String pattern) {
    return Pattern.compile(pattern).matcher(id).matches();
  }
}
