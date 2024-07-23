package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

import java.util.Map;

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
}
