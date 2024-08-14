package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

public class SBOParameters {

    @JsonProperty("omit-generic-terms")
    protected boolean omitGenericTerms = ModelPolisherOptions.ADD_GENERIC_TERMS.getDefaultValue();

    public SBOParameters() {
    }

    public SBOParameters(SBProperties args) {
        omitGenericTerms = args.getBooleanProperty(ModelPolisherOptions.ADD_GENERIC_TERMS);
    }

    public boolean addGenericTerms() {
        return omitGenericTerms;
    }

}
