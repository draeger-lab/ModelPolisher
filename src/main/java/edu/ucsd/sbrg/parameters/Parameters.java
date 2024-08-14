package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Parameters {

    @JsonProperty("polishing")
    private final PolishingParameters polishing = new PolishingParameters();
    @JsonProperty("annotation")
    private final AnnotationParameters annotation = new AnnotationParameters();
    @JsonProperty("sbo-terms")
    private final SBOParameters sboTerms = new SBOParameters();
    @JsonProperty("sbml-validation")
    protected final boolean sbmlValidation = ModelPolisherOptions.SBML_VALIDATION.getDefaultValue();
    @JsonProperty("outputType")
    protected final ModelPolisherOptions.OutputType outputType = ModelPolisherOptions.OUTPUT_TYPE.getDefaultValue();

    public Parameters() {}

    public PolishingParameters polishing() {
        return polishing;
    }

    public AnnotationParameters annotation() {
        return annotation;
    }

    public SBOParameters sboTerms() {
        return sboTerms;
    }

    public boolean sbmlValidation() {
        return sbmlValidation;
    }

    public ModelPolisherOptions.OutputType outputType() {
        return outputType;
    }
}
