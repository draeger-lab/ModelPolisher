package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import edu.ucsd.sbrg.ModelPolisherOptions;

public class SBOParameters {

    @JsonProperty("omit-generic-terms")
    protected boolean omitGenericTerms = ModelPolisherOptions.OMIT_GENERIC_TERMS.getDefaultValue();

    public SBOParameters() {
    }

    public SBOParameters(SBProperties args) {
        omitGenericTerms = args.getBooleanProperty(ModelPolisherOptions.OMIT_GENERIC_TERMS);
    }

    public boolean omitGenericTerms() {
        return omitGenericTerms;
    }

}
