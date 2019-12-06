package edu.ucsd.sbrg.miriam;

import edu.ucsd.sbrg.miriam.models.Miriam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Registry {

  /**
   * A {@link Logger} for this class.
   */
  static final transient Logger logger = Logger.getLogger(Registry.class.getName());
  /**
   * Stores primary URI to regex pattern mapping
   */
  private static HashMap<String, Entry> entries = new HashMap<>();

  private static class Entry {

    private String identifiersURI;
    private String regexPattern;
    private List<String> URIs;


    Entry(List<String> URIs, String regexPattern) {
      this.URIs = URIs;
      this.regexPattern = regexPattern;
      extractPrimary();
    }


    private void extractPrimary() {
      for (String URI : URIs) {
        if (Pattern.matches("http://identifiers\\.org/.*", URI)) {
          identifiersURI = URI;
          break;
        }
      }
    }


    String getPrimaryURI() {
      if (identifiersURI == null) {
        return URIs.get(0);
      } else {
        return identifiersURI;
      }
    }


    String getPattern() {
      return regexPattern;
    }


    boolean checkPattern(String query) {
      return Pattern.matches(regexPattern, query);
    }
  }

  // only extract necessary information and use a static initializer to fill entries
  static {
    Miriam miriam = RegistryProvider.getInstance().getMiriam();
    for (Miriam.Datatype datatype : miriam.getDatatype()) {
      String pattern = datatype.getPattern();
      List<String> stringURIs = new ArrayList<>();
      // ugly xjc generated Classes, a custom wrapper might be better here
      for (Uris uris : datatype.getUris()) {
        for (Uri uri : uris.getUri()) {
          stringURIs.add(uri.getValue());
        }
      }
      Entry entry = new Entry(stringURIs, pattern);
      entries.put(getDataCollectionPartFromURI(entry.getPrimaryURI()), entry);
    }
    RegistryProvider.close();
  }


  /**
   * @param query:
   *        Identifier part of identifiers.org URI
   * @param collection:
   *        Collection part of identifiers.org URI
   * @return
   */
  public static Boolean checkPattern(String query, String collection) {
    if (entries.containsKey(collection)) {
      return entries.get(collection).checkPattern(query);
    } else {
      return false;
    }
  }


  /**
   * Get pattern from collection name
   * 
   * @param collection
   * @return
   */
  public static String getPattern(String collection) {
    if (entries.containsKey(collection)) {
      return entries.get(collection).getPattern();
    } else {
      return "";
    }
  }


  /**
   * @param queryURI:
   *        Non identifiers.org URI
   * @return identifiers.org URI, if found, else empty String
   */
  public static String getCollectionFor(String queryURI) {
    Pattern pattern = Pattern.compile("/\\w+\\.\\w+\\.\\w+/");
    Matcher matcher = pattern.matcher(queryURI);
    if (matcher.find()) {
      String collection = matcher.group(0);
      // assuming www.*.*
      collection = collection.split("\\.")[1];
      if (entries.containsKey(collection)) {
        return collection;
      }
    }
    return "";
  }


  /**
   * Get collection from identifiers.org URI
   * 
   * @param resource:
   *        identifiers.org URI
   * @return
   */
  public static String getDataCollectionPartFromURI(String resource) {
    if (resource.contains("identifiers.org")) {
      return resource.split("/")[3];
    } else {
      return resource.split("/")[2];
    }
  }


  /**
   * Get identifier from identifiers.org URI
   *
   * @param resource:
   *        Full identifiers.org URI
   * @return
   */
  public static String getIdentifierFromURI(String resource) {
    String identifiersURL = "identifiers.org";
    if (resource.contains(identifiersURL)) {
      // We know where the id should be in identifiers.org URLs
      resource = resource.substring(resource.indexOf(identifiersURL) + identifiersURL.length() + 1);
      return resource.substring(resource.indexOf("/")+1);
    } else {
      // assume last part after slash is ID
      String[] split = resource.split("/");
      int len = split.length;
      return split[len - 1];
    }
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
   * Builds full identifiers.org URI from collection and identifier
   * 
   * @param collection:
   *        Collection part of identifiers.org URI
   * @param id:
   *        Identifier part of identifiers.org URI
   * @return
   */
  public static String getURI(String collection, String id) {
    String base = "http://identifiers.org/";
    return base + collection + "/" + id;
  }
}
