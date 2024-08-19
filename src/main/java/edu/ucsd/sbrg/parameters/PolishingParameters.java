package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.fixing.FixingOptions;
import edu.ucsd.sbrg.polishing.PolishingOptions;

import java.util.Objects;

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
