package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

import java.util.Map;
import java.util.Objects;

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

    @Override
    public String toString() {
        return "ReactionPolishingParameters{" +
                "checkMassBalance=" + checkMassBalance +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactionPolishingParameters that = (ReactionPolishingParameters) o;
        return checkMassBalance == that.checkMassBalance;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(checkMassBalance);
    }
}
