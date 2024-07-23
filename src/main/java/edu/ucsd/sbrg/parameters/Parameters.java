package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.ucsd.sbrg.ModelPolisherOptions;

public class Parameters {

    @JsonProperty("polishing")
    private PolishingParameters polishing = new PolishingParameters();
    @JsonProperty("annotation")
    private AnnotationParameters annotation = new AnnotationParameters();
    @JsonProperty("sbo-terms")
    private SBOParameters sboTerms = new SBOParameters();
    @JsonProperty("output")
    private OutputParameters output = new OutputParameters();
    @JsonProperty("sbml-validation")
    protected boolean sbmlValidation = ModelPolisherOptions.SBML_VALIDATION.getDefaultValue();

    public PolishingParameters polishing() {
        return polishing;
    }

    public AnnotationParameters annotation() {
        return annotation;
    }

    public SBOParameters sboTerms() {
        return sboTerms;
    }

    public OutputParameters output() {
        return output;
    }

    public boolean sbmlValidation() {
        return sbmlValidation;
    }
}
