package edu.ucsd.sbrg.miriam.models;

import java.util.Calendar;
import java.util.List;

public class Namespace {
  private long id;
  private String mirId;
  private String prefix;
  private String name;
  private String description;
  private String pattern;
  private Calendar created;
  private Calendar modified;
  private String sampleId;
  private boolean namespaceEmbeddedInLui;
  private List<Resource> resources;
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

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
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

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public Calendar getCreated() {
    return created;
  }

  public void setCreated(Calendar created) {
    this.created = created;
  }

  public Calendar getModified() {
    return modified;
  }

  public void setModified(Calendar modified) {
    this.modified = modified;
  }

  public String getSampleId() {
    return sampleId;
  }

  public void setSampleId(String sampleId) {
    this.sampleId = sampleId;
  }

  public boolean isNamespaceEmbeddedInLui() {
    return namespaceEmbeddedInLui;
  }

  public void setNamespaceEmbeddedInLui(boolean namespaceEmbeddedInLui) {
    this.namespaceEmbeddedInLui = namespaceEmbeddedInLui;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources;
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
