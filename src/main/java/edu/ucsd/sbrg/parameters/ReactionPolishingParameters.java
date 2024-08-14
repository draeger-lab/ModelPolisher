package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

import java.util.Map;

public class ReactionPolishingParameters {

    public static final String CHECK_MASS_BALANCE = "checkMassBalance";

    @JsonProperty("check-mass-balance")
    protected boolean checkMassBalance = ModelPolisherOptions.CHECK_MASS_BALANCE.getDefaultValue();

    public ReactionPolishingParameters() {
    }

    public ReactionPolishingParameters(SBProperties args) {
        checkMassBalance = args.getBooleanProperty(ModelPolisherOptions.CHECK_MASS_BALANCE);
    }

    public ReactionPolishingParameters(Map<String, Object> params) {
        checkMassBalance = (boolean) params.getOrDefault(CHECK_MASS_BALANCE,
                ModelPolisherOptions.CHECK_MASS_BALANCE.getDefaultValue());
    }

    public boolean checkMassBalance() {
        return checkMassBalance;
    }



}
