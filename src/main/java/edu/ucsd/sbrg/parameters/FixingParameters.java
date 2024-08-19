package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.fixing.FixingOptions;

import java.util.Objects;

public class FixingParameters {

    @JsonProperty("dont-fix")
    private boolean dontFix = false;
    @JsonProperty("fix-flux-objectives")
    private FluxObjectivesFixingParameters fluxObjectivesFixingParameters = new FluxObjectivesFixingParameters();

    public FixingParameters() {
    }

    public FixingParameters(boolean dontFix, FluxObjectivesFixingParameters fluxObjectivesFixingParameters) {
        this.dontFix = dontFix;
        this.fluxObjectivesFixingParameters = fluxObjectivesFixingParameters;
    }


    public FixingParameters(SBProperties args) throws IllegalArgumentException {
        this.dontFix = args.getBooleanProperty(FixingOptions.DONT_FIX);
        this.fluxObjectivesFixingParameters = new FluxObjectivesFixingParameters(args);
    }

    public boolean dontFix() {
        return dontFix;
    }

    public FluxObjectivesFixingParameters fluxObjectivesPolishingParameters() {
        return fluxObjectivesFixingParameters;
    }

    @Override
    public String toString() {
        return "FixingParameters{" +
                "dontFix=" + dontFix +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FixingParameters that = (FixingParameters) o;
        return dontFix == that.dontFix;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dontFix);
    }
}
