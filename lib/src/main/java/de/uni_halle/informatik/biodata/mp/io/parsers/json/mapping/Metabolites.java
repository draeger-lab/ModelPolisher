package de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Metabolites {

  private Map<String, Double> metabolites = new HashMap<>();

  @JsonAnySetter
  public void add(String key, double value) {
    metabolites.put(key, value);
  }


  @JsonAnyGetter
  public Map<String, Double> get() {
    return metabolites;
  }


  @JsonSetter
  public void set(Map<String, Double> metabolites) {
    this.metabolites = metabolites;
  }
}
