package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

import java.util.Objects;

public class PolishingParameters {

  @JsonProperty("polish-even-if-model-invalid")
  private boolean polishEvenIfModelInvalid = false;
  @JsonProperty("reactions")
  private ReactionPolishingParameters reactionPolishingParameters = new ReactionPolishingParameters();
  @JsonProperty("flux-objectives")
  private FluxObjectivesPolishingParameters fluxObjectivesPolishingParameters = new FluxObjectivesPolishingParameters();

  public PolishingParameters() {  }

  public PolishingParameters(ReactionPolishingParameters reactionPolishingParameters,
                             FluxObjectivesPolishingParameters fluxObjectivesPolishingParameters,
                             boolean polishEvenIfModelInvalid) {
    this.reactionPolishingParameters = reactionPolishingParameters;
    this.fluxObjectivesPolishingParameters = fluxObjectivesPolishingParameters;
    this.polishEvenIfModelInvalid = polishEvenIfModelInvalid;
  }

  public PolishingParameters(SBProperties args) throws IllegalArgumentException {
    reactionPolishingParameters = new ReactionPolishingParameters(args);
    fluxObjectivesPolishingParameters = new FluxObjectivesPolishingParameters(args);
  }

  public ReactionPolishingParameters reactionPolishingParameters() {
    return reactionPolishingParameters;
  }

  public FluxObjectivesPolishingParameters fluxObjectivesPolishingParameters() {
    return fluxObjectivesPolishingParameters;
  }

  public boolean polishEvenIfModelInvalid() {
    return polishEvenIfModelInvalid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PolishingParameters that = (PolishingParameters) o;
    return polishEvenIfModelInvalid == that.polishEvenIfModelInvalid && Objects.equals(reactionPolishingParameters, that.reactionPolishingParameters) && Objects.equals(fluxObjectivesPolishingParameters, that.fluxObjectivesPolishingParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(polishEvenIfModelInvalid, reactionPolishingParameters, fluxObjectivesPolishingParameters);
  }

  @Override
  public String toString() {
    return "PolishingParameters{" +
            "polishEvenIfModelInvalid=" + polishEvenIfModelInvalid +
            ", reactionPolishingParameters=" + reactionPolishingParameters +
            ", fluxObjectivesPolishingParameters=" + fluxObjectivesPolishingParameters +
            '}';
  }

}
