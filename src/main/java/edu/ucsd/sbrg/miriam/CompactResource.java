package edu.ucsd.sbrg.miriam;

import edu.ucsd.sbrg.miriam.models.Resource;

public class CompactResource {

  private final long id;
  private final String providerCode;
  private final String urlPattern;

  private CompactResource(Resource resource) {
    id = resource.getId();
    providerCode = resource.getProviderCode();
    urlPattern = resource.getUrlPattern();
  }


  public static CompactResource fromResource(Resource resource) {
    return new CompactResource(resource);
  }


  public long getId() {
    return id;
  }


  public String getProviderCode() {
    return providerCode;
  }


  public String getUrlPattern() {
    return urlPattern;
  }
}
