package edu.ucsd.sbrg.parsers.models;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class Compartments {

  private static final Logger logger = Logger.getLogger(Compartments.class.getName());
  private Map<String, String> compartments = new HashMap<>();

  @JsonAnySetter
  public void add(String key, String value) {
    if (key.isEmpty()) {
      return;
    }
    Pattern validCompartmentCode = Pattern.compile("[a-z]{1,2}");
    if (validCompartmentCode.matcher(key).find()) {
      compartments.put(key, value);
    } else {
      logger.warning(String.format("Compartment code '%s' did not match required pattern [a-z]{1,2}", key));
    }
  }


  @JsonGetter
  public Map<String, String> get() {
    return compartments;
  }


  @JsonSetter
  public void set(Map<String, String> compartments) {
    this.compartments = compartments;
  }
}
