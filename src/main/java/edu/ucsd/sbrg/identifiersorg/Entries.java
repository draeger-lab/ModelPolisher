package edu.ucsd.sbrg.identifiersorg;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * The singleton {@code Entries} class serves as a central repository for managing and querying MIRIAM namespaces and their associated resources. 
 * This class provides functionalityto initialize from a list of compact entries, 
 * retrieve collections based on URLs or provider codes, and perform pattern matching to resolve URLs to specific MIRIAM collections.
 * 
 * MIRIAM namespaces are used to standardize the annotation of biological models, ensuring that each annotated element
 * is described using a consistent and recognizable format. This class helps in managing these namespaces and provides
 * methods to query and retrieve information about them.
 */
public class Entries {

  /**
   * A {@link Logger} for this class.
   */
  private static final Logger logger = Logger.getLogger(Entries.class.getName());

  private static final Map<String, String> COLLECTION_FOR_PROVIDER = new HashMap<>();
  private static final Map<String, String> PATTERN_FOR_COLLECTION = new HashMap<>();
  private static final Map<String, String> PROVIDER_FOR_COLLECTION = new HashMap<>();
  private static Entries instance;
  private final Root root;

  static class Root {

    private final List<Namespace> children;

    /**
     * Container for reduced representation tree structure.
     * Each child corresponds to a MIRIAM namespace
     *
     * @param entries
     *        entries to convert into children {@link Namespace}s
     */
    Root(List<CompactEntry> entries) {
      children = new ArrayList<>();
      for (CompactEntry entry : entries) {
        String collectionName = entry.getName();
        String pattern = entry.getPattern();
        String prefix = entry.getPrefix();
        COLLECTION_FOR_PROVIDER.put(prefix, collectionName);
        PATTERN_FOR_COLLECTION.put(collectionName, pattern);
        PROVIDER_FOR_COLLECTION.put(collectionName, prefix);
        Namespace child = new Namespace(this, entry);
        children.add(child);
      }
    }
  }

  private Entries(List<CompactEntry> entries) {
    root = new Root(entries);
  }


  /**
   * Builds namespace tree from reduced representation entries and returns an instance to query respective MIRIAM
   * namespaces/resources
   *
   * @param entries
   *        List of {@link CompactEntry} objects representing MIRIAM namespaces reduced to attributes and fields used in
   *        ModelPolisher
   */
  public static void initFromList(List<CompactEntry> entries) {
    if (instance != null) {
      throw new IllegalStateException("Entries have already been initialized!");
    }
    instance = new Entries(entries);
  }


  /**
   * @return List of {@link Namespace} objects representing all MIRIAM namespaces
   */
  public List<Namespace> get() {
    return instance.root.children;
  }


  /**
   * Retrieves collection name for a given annotation url, if uniquely resolvable
   *
   * @param url
   *        annotation url to get a collection name for
   * @return collection name, if uniquely resolvable, else {@code null}
   */
  public Node getCollection(String url) {
    List<Node> matches = getMatchForUrl(url);
    if (matches.size() != 1) {
      return null;
    }
    return matches.get(0);
  }


  /**
   * Retrieve collection name based on provider code
   *
   * @param provider
   *        provider code used as key for lookup
   * @return collection name, if present, else an empty {@link String}
   */
  public String getCollectionForProvider(String provider) {
    return COLLECTION_FOR_PROVIDER.getOrDefault(provider, "");
  }


  /**
   * @return {@link Entries} class instance to query namespaces and associated resources
   */
  public static Entries getInstance() {
    if (instance == null) {
      throw new IllegalStateException("Entries have not been initialized yet.");
    }
    return instance;
  }


  /**
   * Attempts to find a unique match for a given URL within the MIRIAM collections by comparing against namespace
   * and resource URL patterns. This method is used to determine the correct MIRIAM collection for additional annotation.
   *
   * @param query The URL to be matched against the identifiers.org namespace and resource URL patterns.
   * @return A list of {@link Node} objects that match the query URL. If no matches are found, the list will be empty.
   *         If more than one match is found, a log message is generated indicating the URL could not be uniquely resolved.
   */
  public List<Node> getMatchForUrl(String query) {
    // Remove the protocol part of the URL to improve matching accuracy
    if (query.startsWith("http://") || query.startsWith("https://")) {
      query = query.replaceAll("^https?://", "");
    }
    List<Node> matches = new ArrayList<>();
    // Iterate over all namespaces to find matches
    for (Namespace namespace : root.children) {
      if (namespace.isMatch(query)) {
        matches.add(namespace);
      }
      // Iterate over all resources within the namespace to find matches
      for (Node resource : namespace.getLeaves()) {
        if (resource.isMatch(query)) {
          matches.add(resource);
        }
      }
    }
    // Log a message if more than one match is found, indicating non-uniqueness
    if (matches.size() > 1) {
      logger.info(format("Could not resolve MIRIAM collection for URL {0} uniquely", query));
    }
    return matches;
  }


  /**
   * Retrieves the provider code associated with a given collection name from the {@code PROVIDER_FOR_COLLECTION} map.
   * In the context of MIRIAM, a provider refers to an entity
   * or database that supplies the data for a specific collection. This method returns the provider code, which is a unique
   * identifier for the provider associated with the specified collection. If the collection name does not have an associated
   * provider code, an empty string is returned.
   *
   * @param collection The name of the collection for which the provider code is to be retrieved.
   * @return The provider code as a String, or an empty string if no provider code is associated with the collection.
   */
  public String getProviderForCollection(String collection) {
    return PROVIDER_FOR_COLLECTION.getOrDefault(collection, "");
  }


  /**
   * Retrieves the regex pattern associated with a given collection name.
   * If the collection name does not have an associated pattern, an empty string is returned.
   *
   * @param collection The name of the collection for which the pattern is to be retrieved.
   * @return The regex pattern as a String, or an empty string if no pattern is associated with the collection.
   */
  public String getPattern(String collection) {
    return PATTERN_FOR_COLLECTION.getOrDefault(collection, "");
  }


  /**
   * Replaces the identifier placeholder "{$id}" in a URL pattern with a specified regex pattern.
   * This method is designed to facilitate the matching of URLs against a dynamic regex pattern that represents
   * an identifier within a MIRIAM namespace or its child resources.
   *
   * The method first attempts to find the "{$id}" placeholder within the provided URL. If found, it splits the URL
   * around this placeholder and reassembles it with the given regex pattern in place of the placeholder. If the
   * placeholder is not found, the URL is returned as is, but quoted to ensure it is treated as a literal string in regex
   * operations.
   *
   * Note: The placeholder "{$id}" can optionally be surrounded by curly braces, which are considered during the
   * replacement but do not affect the functionality.
   *
   * @param url The URL pattern containing the "{$id}" placeholder. This pattern represents a MIRIAM namespace or a related child namespace.
   * @param pattern The regex pattern that should replace the "{$id}" placeholder in the URL pattern.
   * @return A string representing the URL with the "{$id}" placeholder replaced by the provided regex pattern. If no placeholder is found, the URL is returned unchanged but quoted.
   */
  static String replaceIdTag(String url, String pattern) {
    Pattern id = Pattern.compile("\\{?\\{\\$id}}?");
    Matcher matcher = id.matcher(url);
    if (!matcher.find()) {
      return Pattern.quote(url);
    }
    String[] parts = url.split(id.pattern());
    String result = Pattern.quote(parts[0]) + pattern;
    if (parts.length == 2) {
      result += Pattern.quote(parts[1]);
    }
    return result;
  }


  /**
   * Replaces the identifier placeholder in a URL pattern with a specified regex pattern, considering a namespace prefix.
   * This method is useful when the URL pattern contains placeholders that need to be dynamically replaced with
   * identifier patterns for pattern matching operations. The method also handles namespace prefixes which are part of
   * the placeholder in the URL.
   *
   * @param url The URL pattern containing the placeholder for the identifier. This pattern represents a MIRIAM namespace or a related child namespace.
   * @param pattern The regex pattern that should replace the identifier placeholder in the URL pattern.
   * @param prefix The namespace prefix that might precede the identifier placeholder in the URL pattern.
   * @return A string representing the URL with the identifier placeholder replaced by the provided regex pattern. If no placeholder is found, the URL is returned unchanged.
   */
  static String replaceIdTagWithPrefix(String url, String pattern, String prefix) {
    Pattern id = Pattern.compile(prefix + "\\{?\\{\\$id}}?");
    Matcher matcher = id.matcher(url);
    if (!matcher.find()) {
      return replaceIdTag(url, pattern);
    }
    String[] parts = url.split(id.pattern());
    String result = Pattern.quote(parts[0]) + pattern;
    if (parts.length == 2) {
      result += Pattern.quote(parts[1]);
    }
    return result;
  }


  /**
   * Retrieves the number of namespaces managed by this Entries instance.
   * This method accesses the 'children' list of the 'root' object, which contains
   * all the Namespace instances, and returns its size.
   *
   * @return The total count of Namespace instances contained in the 'children' list of 'root'.
   */
  public int size() {
    return root.children.size();
  }
}
