package edu.ucsd.sbrg.parsers.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Gene {

  @JsonProperty(required = true)
  private String id;
  @JsonProperty(required = true)
  private String name;
  private Notes notes;
  private Annotation annotation;

  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public Notes getNotes() {
    return notes;
  }


  public void setNotes(Notes notes) {
    this.notes = notes;
  }


  public Annotation getAnnotation() {
    return annotation;
  }


  public void setAnnotation(Annotation annotation) {
    this.annotation = annotation;
  }
}
