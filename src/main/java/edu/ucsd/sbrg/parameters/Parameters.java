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
    @JsonProperty("sbml-validation")
    protected boolean sbmlValidation = ModelPolisherOptions.SBML_VALIDATION.getDefaultValue();
    @JsonProperty("outputType")
    protected ModelPolisherOptions.OutputType outputType = ModelPolisherOptions.OUTPUT_TYPE.getDefaultValue();

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
