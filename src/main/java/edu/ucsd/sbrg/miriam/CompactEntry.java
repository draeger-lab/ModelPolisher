package edu.ucsd.sbrg.miriam;

import java.util.List;
import java.util.stream.Collectors;

import edu.ucsd.sbrg.miriam.models.Namespace;

public class CompactEntry {

  private final long id;
  private final String name;
  private final String pattern;
  private final String prefix;
  private final List<CompactResource> resources;

  private CompactEntry(Namespace namespace) {
    id = namespace.getId();
    name = namespace.getName();
    pattern = namespace.getPattern();
    prefix = namespace.getPrefix();
    // Get list of all alternative providers with URLs
    resources =
      namespace.getResources().parallelStream().map(CompactResource::fromResource).collect(Collectors.toList());
  }


  public static CompactEntry fromNamespace(Namespace namespace) {
    return new CompactEntry(namespace);
  }


  public long getId() {
    return id;
  }


  public String getName() {
    return name;
  }


  public String getPattern() {
    return pattern;
  }


  public String getPrefix() {
    return prefix;
  }


  public List<CompactResource> getResources() {
    return resources;
  }
}
