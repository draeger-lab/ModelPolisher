package de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping;

import static java.text.MessageFormat.format;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Compartments {

  private static final Logger logger = LoggerFactory.getLogger(Compartments.class);
  private Map<String, String> compartments = new HashMap<>();

  @JsonAnySetter
  public void add(String key, String value) {
    if (key.isEmpty()) {
      return;
    }
    Pattern validCompartmentCode = Pattern.compile("(C_)?[a-z]{1,2}");
    if (validCompartmentCode.matcher(key).find()) {
      if (key.startsWith("C_")) {
        key = key.substring(2);
      }
      compartments.put(key, value);
    } else {
      logger.debug(format("Compartment code {0} did not match required pattern (C_)?[a-z]'{'1,2'}'", key));
    }
  }


  /**
   */
  public void addAll(Map<String, String> comps) {
    comps.forEach(this::add);
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
