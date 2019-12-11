package edu.ucsd.sbrg.parsers.models;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class Annotation {
  private Map<String, Object> annotation = new HashMap<>();


  @JsonAnySetter
  public void addAnnotation(String key, Object value) {
    annotation.put(key, value);
  }

  public Map<String, Object> getAnnotation() {
    return annotation;
  }

  public void setAnnotation(Map<String, Object> annotation) {
    this.annotation = annotation;
  }


}
