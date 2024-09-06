package de.uni_halle.informatik.biodata.mp.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

import java.util.Objects;

public class SBOParameters {

    @JsonProperty("add-generic-terms")
    protected boolean addGenericTerms = GeneralOptions.ADD_GENERIC_TERMS.getDefaultValue();

    public SBOParameters() {
    }

    public SBOParameters(SBProperties args) {
        addGenericTerms = args.getBooleanProperty(GeneralOptions.ADD_GENERIC_TERMS);
    }

    public boolean addGenericTerms() {
        return addGenericTerms;
    }

    @Override
    public String toString() {
        return "SBOParameters{" +
                "addGenericTerms=" + addGenericTerms +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SBOParameters that = (SBOParameters) o;
        return addGenericTerms == that.addGenericTerms;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(addGenericTerms);
    }
}
