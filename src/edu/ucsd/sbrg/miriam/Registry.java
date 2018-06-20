package edu.ucsd.sbrg.miriam;

import java.util.HashMap;
import java.util.regex.Pattern;

import edu.ucsd.sbrg.miriam.xjc.Miriam;
import edu.ucsd.sbrg.miriam.xjc.Uri;
import edu.ucsd.sbrg.miriam.xjc.Uris;

public class Registry {

  private static HashMap<String, String> datatypes = new HashMap<>();
  // only extract necessary information
  static {
    Miriam miriam = RegistryProvider.getInstance().getMiriam();
    for (Miriam.Datatype datatype : miriam.getDatatype()) {
      String pattern = datatype.getPattern();
      // ugly xjc generated Classes, a custom wrapper might be better here
      for (Uris uris : datatype.getUris()) {
        for (Uri uri : uris.getUri()) {
          String uriAddress = uri.getValue();
          if (uriAddress == null) {
            continue;
          }
          if (uri.getType().value().equals("URL") && (uri.isDeprecated() == null || !uri.isDeprecated())
            && uriAddress.contains("identifiers.org")) {
            putPattern(uriAddress, pattern);
          }
        }
      }
    }
    RegistryProvider.close();
  }


  private static void putPattern(String uriAddress, String pattern) {
    if (!datatypes.containsKey(uriAddress)) {
      datatypes.put(uriAddress, pattern);
    }
  }


  public static Boolean checkPattern(String query, String collection) {
    String pattern = getPattern(collection);
    if (pattern.equals("")) {
      // TODO: log pattern not found
      return false;
    }
    String[] splits = query.split("/");
    int len = splits.length;
    query = splits[len - 1];
    return Pattern.matches(pattern, query);
  }


  public static String getPattern(String collection) {
    collection = getURI(collection, "");
    return datatypes.getOrDefault(collection, "");
  }


  public static String getDataCollectionPartFromURI(String resource) {
    String[] split = resource.split("/");
    int len = split.length;
    return split[len - 2];
  }


  public static String getIdentifierFromURI(String resource) {
    String[] split = resource.split("/");
    int len = split.length;
    return split[len - 1];
  }


  public static String replace(String resource, String pattern, String replacement) {
    return resource.replaceAll(pattern, replacement);
  }


  public static String getURI(String collection, String id) {
    String base = "http://identifiers.org/";
    return base + collection + "/" + id;
  }
}
