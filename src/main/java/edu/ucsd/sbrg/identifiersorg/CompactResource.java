package edu.ucsd.sbrg.identifiersorg;

import edu.ucsd.sbrg.identifiersorg.models.Resource;

/**
 * Represents a compact version of a Resource with essential fields only.
 * This class is used to store and manage a minimal set of data attributes
 * derived from a Resource object, which are necessary for specific operations
 * or functionalities within the application.
 */
public class CompactResource {

  /**
   * Unique identifier for the resource.
   */
  private final long id;

  /**
   * Code representing the provider of the resource.
   */
  private final String providerCode;

  /**
   * Sample identifier associated with the resource.
   */
  private final String sampleId;

  /**
   * URL pattern that can be used to access the resource online.
   */
  private final String urlPattern;

  /**
   * Flag indicating whether the resource is deprecated.
   */
  private final boolean deprecated;

  /**
   * Flag indicating whether the resource is officially recognized or supported.
   */
  private final boolean official;

  /**
   * Private constructor that initializes a CompactResource object using a Resource instance.
   * 
   * @param resource The Resource object from which to extract properties.
   */
  private CompactResource(Resource resource) {
    id = resource.getId();
    providerCode = resource.getProviderCode();
    sampleId = resource.getSampleId();
    urlPattern = resource.getUrlPattern();
    deprecated = resource.isDeprecated();
    official = resource.isOfficial();
  }

  /**
   * Factory method to create a CompactResource instance from a Resource object.
   * 
   * @param resource The Resource object to convert.
   * @return A new instance of CompactResource containing the essential fields from the given Resource.
   */
  public static CompactResource fromResource(Resource resource) {
    return new CompactResource(resource);
  }

  /**
   * Gets the unique identifier for this resource.
   * 
   * @return The unique identifier.
   */
  public long getId() {
    return id;
  }

  /**
   * Gets the provider code of this resource.
   * 
   * @return The provider code.
   */
  public String getProviderCode() {
    return providerCode;
  }

  /**
   * Gets the sample identifier of this resource.
   * 
   * @return The sample identifier.
   */
  public String getSampleId() {
    return sampleId;
  }

  /**
   * Gets the URL pattern of this resource.
   * 
   * @return The URL pattern.
   */
  public String getUrlPattern() {
    return urlPattern;
  }

  /**
   * Checks if this resource is deprecated.
   * 
   * @return True if the resource is deprecated, otherwise false.
   */
  public boolean isDeprecated() {
    return deprecated;
  }

  /**
   * Checks if this resource is officially recognized or supported.
   * 
   * @return True if the resource is official, otherwise false.
   */
  public boolean isOfficial() {
    return official;
  }
}
