package edu.ucsd.sbrg.miriam;

import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.miriam.models.Miriam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;
import static java.text.MessageFormat.format;

public class Registry {

  /**
   * A {@link Logger} for this class.
   */
  static final transient Logger logger = Logger.getLogger(Registry.class.getName());
  /**
   *
   */
  private static final List<CompactEntry> entries;
  /**
   * Contains mapping from resource provider URL to collection name
   */
  private static final Map<String, String> alternativeURI = new HashMap<>();
  /**
   * Contains mapping for identifiers.org URI /w & /wo provider information
   */
  private static final Map<String, String> collectionForUri = new HashMap<>();
  /**
   * Mapping for collection to its pattern
   */
  private static final Map<String, String> collectionForPattern = new HashMap<>();
  /**
   * Mapping provider code to collection name
   */
  private static final Map<String, String> collectionForPrefix = new HashMap<>();
  /**
   * Mapping for pattern to its respective collection
   */
  private static final Map<String, String> patternForCollection = new HashMap<>();
  /**
   * Mapping collection name to provider code
   */
  private static final Map<String, String> prefixForCollection = new HashMap<>();

  /*
   * Static initializer for Miriam, read registry once and convert to compact representation
   */
  static {
    Miriam miriam = RegistryProvider.getInstance().getMiriam();
    // convert namespaces to CompactEntries, holding only the necessary information for ModelPolisher
    entries =
        miriam.getNamespaces().values().parallelStream().map(CompactEntry::fromNamespace).collect(Collectors.toList());
    // Free unneeded resources for GC
    RegistryProvider.close();
    // Create helper structures for fast access
    for (CompactEntry entry : entries) {
      String collectionName = entry.getName();
      String pattern = entry.getPattern();
      String prefix = entry.getPrefix();
      collectionForPattern.put(pattern, collectionName);
      collectionForPrefix.put(prefix, collectionName);
      collectionForUri.put(createProviderURI(prefix), collectionName);
      patternForCollection.put(collectionName, pattern);
      prefixForCollection.put(collectionName, prefix);
      for (CompactResource resource : entry.getResources()) {
        String provider = resource.getProviderCode();
        alternativeURI.put(resource.getUrlPattern(), collectionName);
        collectionForUri.put(createProviderURI(provider), collectionName);
      }
    }
  }

  /**
   * @param providerCode
   * @param id
   * @return
   */
  public static String createURI(String providerCode, BiGGId id) {
    return createURI(providerCode, id.getAbbreviation());
  }


  /**
   * @param id
   * @return
   */
  public static String createShortURI(Object id) {
    return "https://identifiers.org/" + id.toString();
  }


  /**
   * @param providerCode
   * @param id
   * @return
   */
  public static String createURI(String providerCode, Object id) {
    return createProviderURI(providerCode) + id.toString();
  }


  /**
   * @param providerCode
   * @return
   */
  public static String createProviderURI(String providerCode) {
    return "https://identifiers.org/" + providerCode + "/";
  }


  /**
   * Checks resource URIs and logs those not matching the specified pattern
   * Used to check URIs obtained from BiGGDB
   * A resource URI that does not match the pattern will be logged and not added
   * to the model
   *
   * @param resource: resource URI to be added as annotation
   * @return corrected resource URI
   */
  public static String checkResourceUrl(String resource) {
    /*
     * Either [namespace prefix]:[accession] or [provider code]/[namespace prefix]:[accession] second option is
     * currently not strict - older URIs with only accession are supported, if provider code is given
     */
    Pattern identifiersURL = Pattern.compile("(?:https?://)?identifiers.org/(?:(?<provider>.*)/)?(?<id>.*)");
    Matcher urlMatcher = identifiersURL.matcher(resource);
    String provider = "";
    String identifier = "";
    if (urlMatcher.matches()) {
      provider = Optional.ofNullable(urlMatcher.group("provider")).orElse("");
      identifier = urlMatcher.group("id");
    } else {
      // Handle both possible cases for non identifiers.org URLs
      if (resource.contains("/")) {
        String url = resource.substring(0, resource.lastIndexOf("/"));
        if (alternativeURI.containsKey(url)) {
          String collection = alternativeURI.get(url);
          provider = prefixForCollection.get(collection);
          identifier = resource.substring(0, resource.lastIndexOf("/"));
        }
      } else if (resource.contains(":")) {
        String url = resource.substring(0, resource.lastIndexOf(":"));
        if (alternativeURI.containsKey(url)) {
          String collection = alternativeURI.get(url);
          provider = prefixForCollection.get(collection);
          identifier = resource.substring(0, resource.lastIndexOf(":"));
        }
      }
    }
    // TODO: check if this results in false positives
    // Get prefix by checking for uniquely matching Regex
    String query = identifier;
    if (provider.isEmpty()) {
      List<String> collections =
          collectionForPattern.keySet().stream().filter(s -> Pattern.compile(s).matcher(query).matches())
              .collect(Collectors.toList());
      if (collections.size() == 1) {
        String collection = collections.get(0);
        provider = prefixForCollection.get(collection);
      }
    }
    if (!collectionForPrefix.containsKey(provider)) {
      logger.severe(format(mpMessageBundle.getString("UNCAUGHT_URI"), resource));
      return resource;
    }
    String collection = collectionForPrefix.get(provider);
    String regexp = getPattern(collection);
    Boolean correct = checkPattern(identifier, regexp);
    String report_resource = resource;
    if (!correct) {
      logger.info(format(mpMessageBundle.getString("PATTERN_MISMATCH_INFO"), identifier, regexp, collection));
      resource = fixResource(resource, identifier);
    }
    if (resource == null) {
      logger.warning(format(mpMessageBundle.getString("CORRECTION_FAILED_DROP"), report_resource, collection));
    }
    logger.fine(format("Added resource {0}", resource));
    return resource;
  }

  /**
   * Get pattern from collection name
   *
   * @param collection
   * @return
   */
  public static String getPattern(String collection) {
    return patternForCollection.getOrDefault(collection, "");
  }

  /**
   * @param id:      Id part of annotation URL
   * @param pattern: Pattern for matching collection
   * @return
   */
  public static Boolean checkPattern(String id, String pattern) {
    return Pattern.compile(pattern).matcher(id).matches();
  }


  /**
   * @param resource
   * @param identifier
   * @return
   */
  private static String fixResource(String resource, String identifier) {
    if (resource.contains("kegg")) {
      // We can correct the kegg collection
      resource = fixKEGGCollection(resource, identifier);
    } else if (resource.contains("ncbigi")) {
      // add possibly missing "gi:" prefix to identifier
      resource = fixGI(resource, identifier);
    } else if (resource.contains("go") && !resource.contains("goa")) {
      resource = fixGO(resource, identifier);
    } else if (resource.contains("ec-code")) {
      resource = fixECCode(resource, identifier);
    } else if (resource.contains("rhea") && resource.contains("#")) {
      // remove last part, it's invalid either way, even though it gets resolved due to misinterpretation as non
      // existing anchor
      resource = resource.split("#")[0];
      logger.info(format(mpMessageBundle.getString("CHANGED_RHEA"), resource));
    } else if (resource.contains("reactome")) {
      String resource_old = resource;
      resource = fixReactome(resource, identifier);
      logger.info(format(mpMessageBundle.getString("CHANGED_REACTOME"), resource_old, resource));
    } else {
      resource = null;
    }
    return resource;
  }


  /**
   * @param resource
   * @param identifier
   * @return
   */
  private static String fixKEGGCollection(String resource, String identifier) {
    if (identifier.startsWith("D")) {
      logger.info(mpMessageBundle.getString("CHANGE_KEGG_DRUG"));
      resource = replace(resource, "kegg.compound", "kegg.drug");
    } else if (identifier.startsWith("G")) {
      logger.info(mpMessageBundle.getString("CHANGE_KEGG_GLYCAN"));
      resource = replace(resource, "kegg.compound", "kegg.glycan");
    }
    return resource;
  }

  /**
   * @param resource
   * @param pattern
   * @param replacement
   * @return
   */
  public static String replace(String resource, String pattern, String replacement) {
    return resource.replaceAll(pattern, replacement);
  }

  /**
   * @param resource
   * @param identifier
   * @return
   */
  private static String fixGI(String resource, String identifier) {
    if (!identifier.toLowerCase().startsWith("gi:")) {
      logger.info(mpMessageBundle.getString("ADD_PREFIX_GI"));
      return replace(resource, identifier, "GI:" + identifier);
    }
    return resource;
  }


  /**
   * @param resource
   * @param identifier
   * @return
   */
  private static String fixGO(String resource, String identifier) {
    if (!identifier.toLowerCase().startsWith("go:")) {
      logger.info(mpMessageBundle.getString("ADD_PREFIX_GO"));
      return replace(resource, identifier, "GO:" + identifier);
    }
    return resource;
  }


  /**
   * @param resource
   * @param identifier
   * @return
   */
  private static String fixECCode(String resource, String identifier) {
    int missingDots = identifier.length() - identifier.replace(".", "").length();
    if (missingDots < 1) {
      logger.warning(format(mpMessageBundle.getString("EC_CHANGE_FAILED"), identifier));
      return null;
    }
    return replace(resource, identifier, identifier + ".-".repeat(Math.max(0, 3 - missingDots)));
  }


  /**
   * @param resource
   * @param identifier
   * @return
   */
  private static String fixReactome(String resource, String identifier) {
    if (!identifier.startsWith("R-ALL-REACT_")) {
      return null;
    }
    identifier = identifier.split("_")[1];
    resource = replace(resource, "R-ALL-REACT_", "");
    String collection = getDataCollectionPartFromURI(resource);
    //fixme
    if (checkPattern(identifier, patternForCollection.get("Reactome"))) {
      return createURI(collection, identifier);
    } else {
      return null;
    }
  }




  /**
   * Get collection from identifiers.org URI
   *
   * @param resource: identifiers.org URI
   * @return
   */
  public static String getDataCollectionPartFromURI(String resource) {
    if (resource.contains("identifiers.org")) {
      return resource.split("/")[3];
    } else {
      return resource.split("/")[2];
    }
  }

}
