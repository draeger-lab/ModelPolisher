package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

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

    @Override
    public String toString() {
        return "Parameters{" +
                "polishing=" + polishing +
                ", annotation=" + annotation +
                ", sboTerms=" + sboTerms +
                ", sbmlValidation=" + sbmlValidation +
                ", outputType=" + outputType +
            '}';    
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameters that = (Parameters) o;
        return sbmlValidation == that.sbmlValidation && Objects.equals(polishing, that.polishing) && Objects.equals(annotation, that.annotation) && Objects.equals(sboTerms, that.sboTerms) && outputType == that.outputType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(polishing, annotation, sboTerms, sbmlValidation, outputType);
    }
}
