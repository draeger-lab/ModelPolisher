package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

import java.util.Objects;

public class PolishingParameters {

  @JsonProperty("reactions")
  private ReactionPolishingParameters reactionPolishingParameters = new ReactionPolishingParameters();
  @JsonProperty("flux-objectives")
  private FluxObjectivesPolishingParameters fluxObjectivesPolishingParameters = new FluxObjectivesPolishingParameters();

  public PolishingParameters() {  }

  public PolishingParameters(ReactionPolishingParameters reactionPolishingParameters,
                             FluxObjectivesPolishingParameters fluxObjectivesPolishingParameters) {
    this.reactionPolishingParameters = reactionPolishingParameters;
    this.fluxObjectivesPolishingParameters = fluxObjectivesPolishingParameters;
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

  @Override
  public String toString() {
    return "PolishingParameters{" +
            "reactionPolishingParameters=" + reactionPolishingParameters +
            ", fluxObjectivesPolishingParameters=" + fluxObjectivesPolishingParameters +
            '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PolishingParameters that = (PolishingParameters) o;
    return Objects.equals(reactionPolishingParameters, that.reactionPolishingParameters) && Objects.equals(fluxObjectivesPolishingParameters, that.fluxObjectivesPolishingParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reactionPolishingParameters, fluxObjectivesPolishingParameters);
  }
}
