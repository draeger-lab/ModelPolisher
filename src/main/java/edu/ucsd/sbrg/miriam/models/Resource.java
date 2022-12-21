package edu.ucsd.sbrg.miriam.models;

import java.util.Calendar;

public class Resource {
  private long id;
  private String mirId;
  private String urlPattern;
  private String name;
  private String description;
  private boolean official;
  private String providerCode;
  private String sampleId;
  private String resourceHomeUrl;
  private Institution institution;
  private Location location;
  private boolean deprecated;
  private Calendar deprecationDate;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getMirId() {
    return mirId;
  }

  public void setMirId(String mirId) {
    this.mirId = mirId;
  }

  public String getUrlPattern() {
    return urlPattern;
  }

  public void setUrlPattern(String urlPattern) {
    this.urlPattern = urlPattern;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isOfficial() {
    return official;
  }

  public void setOfficial(boolean official) {
    this.official = official;
  }

  public String getProviderCode() {
    return providerCode;
  }

  public void setProviderCode(String providerCode) {
    this.providerCode = providerCode;
  }

  public String getSampleId() {
    return sampleId;
  }

  public void setSampleId(String sampleId) {
    this.sampleId = sampleId;
  }

  public String getResourceHomeUrl() {
    return resourceHomeUrl;
  }

  public void setResourceHomeUrl(String resourceHomeUrl) {
    this.resourceHomeUrl = resourceHomeUrl;
  }

  public Institution getInstitution() {
    return institution;
  }

  public void setInstitution(Institution institution) {
    this.institution = institution;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public boolean isDeprecated() {
    return deprecated;
  }

  public void setDeprecated(boolean deprecated) {
    this.deprecated = deprecated;
  }

  public Calendar getDeprecationDate() {
    return deprecationDate;
  }

  public void setDeprecationDate(Calendar deprecationDate) {
    this.deprecationDate = deprecationDate;
  }
}
