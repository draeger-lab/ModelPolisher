package de.uni_halle.informatik.biodata.mp.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import de.uni_halle.informatik.biodata.mp.polishing.PolishingOptions;

public class PolishingParameters {

  @JsonProperty("polish-even-if-model-invalid")
  private boolean polishEvenIfModelInvalid = false;

  public PolishingParameters() {  }

  public PolishingParameters(boolean polishEvenIfModelInvalid) {
    this.polishEvenIfModelInvalid = polishEvenIfModelInvalid;
  }

  public PolishingParameters(SBProperties args) throws IllegalArgumentException {
    this.polishEvenIfModelInvalid = args.getBooleanProperty(PolishingOptions.POLISH_EVEN_IF_MODEL_INVALID);
  }

  public boolean polishEvenIfModelInvalid() {
    return polishEvenIfModelInvalid;
  }

}
