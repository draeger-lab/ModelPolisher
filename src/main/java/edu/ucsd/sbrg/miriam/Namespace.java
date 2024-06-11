
package edu.ucsd.sbrg.miriam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Represents a MIRIAM namespace entry, encapsulating the essential data required by ModelPolisher.
 * A namespace in MIRIAM is a collection of identifiers under a common context, typically representing
 * a specific database or a type of biological entity. This class provides methods to manage and interact
 * with these identifiers, including pattern matching, URL resolution, and checking deprecation status.
 */
class Namespace implements Node {

  private final Entries.Root parent;
  private final CompactEntry entry;
  private final List<Resource> children;

  Namespace(Entries.Root parent, CompactEntry entry) {
    this.parent = parent;
    this.entry = entry;
    children = new ArrayList<>();
    for (CompactResource resource : entry.getResources()) {
      // children correspond to namespace resources
      Resource child = new Resource(this, resource);
      children.add(child);
    }
  }


  /**
   * Retrieves the list of resources associated with this namespace.
   * 
   * @return List of resources for a namespace
   */
  public List<Resource> getLeaves() {
    return children;
  }

  public String getName(){
    return entry.getName();
  }

  public String getPattern() {
    return entry.getPattern();
  }


  public String getPrefix() {
    return entry.getPrefix();
  }


  public String getSampleId() {
    return entry.getSampleId();
  }


  /**
   * Determines if the namespace is embedded within the locally unique identifier (LUI).
   * 
   * @return {@code true} if the namespace is embedded in the LUI, {@code false} otherwise.
   */
  public boolean isNamespaceEmbeddedInLui() {
    return entry.isNamespaceEmbeddedInLui();
  }

  
  public boolean matchesPattern(String query) {
    return Pattern.compile(getPattern()).matcher(query).matches();
  }


  @Override
  public Optional<String> extractId(String query) {
    String target = getURLWithPattern();
    if (query.startsWith("http://") || query.startsWith("https://")) {
      query = query.replaceAll("^https?://", "");
    }
    Matcher matcher = Pattern.compile(target).matcher(query);
    if (matcher.find()) {
      return Optional.of(matcher.group("id"));
    }
    return Optional.empty();
  }


  @Override
  public String getURLWithPattern() {
    String idPattern = getPattern().replaceAll("\\^|\\$", "");
    if (idPattern.equals("HGVP\\d+")) {
      // fix for wrong gwascentral phenotype RegEx
      idPattern = "HGVPM\\d+";
    } else if (idPattern.equals("G|P|U|C|S\\d{5}")) {
      idPattern = "(G|P|U|C|S)\\d{5}";
    }
    idPattern = "(?<id>" + idPattern + ")";
    return "identifiers.org/" + getPrefix() + "/" + idPattern;
  }

  /**
   * Resolves the full URL for a given identifier based on whether the namespace is embedded in the LUI.
   * If the namespace is embedded, it extracts the prefix pattern from the namespace pattern and constructs the URL.
   * If the namespace is not embedded, it directly appends the identifier to the base URL.
   *
   * @param id The identifier to be resolved.
   * @return The fully resolved URL as a String.
   * @throws IllegalStateException if the prefix cannot be extracted when expected.
   */
  @Override
  public String resolveID(String id) {
    if (isNamespaceEmbeddedInLui()) {
      // Compile a pattern to extract the prefix from the namespace pattern.
      Pattern prefixPattern = Pattern.compile("\\(?[\\w\\\\]+?[:]\\)?");
        // Quickfix for escaped colon in CCO and possibly others
      Matcher prefixMatcher = prefixPattern.matcher(getPattern().replaceAll("\\^|\\$", ""));
      if (prefixMatcher.find()) {
        String pattern = prefixMatcher.group();
        // Handle the special case where the colon might be escaped in the pattern.
        if (pattern.endsWith("\\:")) {
          pattern = pattern.substring(0, pattern.length() - 2) + ":";
        }
        return "https://identifiers.org/" + getPrefix() + "/" + pattern + id;
      }
      throw new IllegalStateException("Could not extract prefix, this should not happen.");
    } else {
      // Construct the URL directly if the namespace is not embedded in the LUI.
      return "https://identifiers.org/" + getPrefix() + "/" + id;
    }
  }


  @Override
  public boolean isDeprecated() {
    return entry.isDeprecated();
  }


  @Override
  public boolean isMatch(String query) {
    String target = getURLWithPattern();
    if (query.startsWith("http://") || query.startsWith("https://")) {
      query = query.replaceAll("^https?://", "");
    }
    Matcher matcher = Pattern.compile(target).matcher(query);
    return matcher.matches();
  }
}
