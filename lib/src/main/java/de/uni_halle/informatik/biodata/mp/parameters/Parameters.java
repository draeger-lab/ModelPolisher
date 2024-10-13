package de.uni_halle.informatik.biodata.mp.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import de.uni_halle.informatik.biodata.mp.io.IOOptions;

import java.util.Objects;

public class Parameters {

    @JsonProperty("fixing")
    private FixingParameters fixing = new FixingParameters();
    @JsonProperty("polishing")
    private PolishingParameters polishing = new PolishingParameters();
    @JsonProperty("annotation")
    private AnnotationParameters annotation = new AnnotationParameters();
    @JsonProperty("sbo-terms")
    private SBOParameters sboParameters = new SBOParameters();
    @JsonProperty("sbml-validation")
    protected boolean sbmlValidation = GeneralOptions.SBML_VALIDATION.getDefaultValue();
    @JsonProperty("outputType")
    protected IOOptions.OutputType outputType = IOOptions.OUTPUT_TYPE.getDefaultValue();

    public Parameters() {}

    public Parameters(SBProperties args) {
        this.fixing = new FixingParameters(args);
        this.polishing = new PolishingParameters(args);
        this.annotation = new AnnotationParameters(args);
        this.sboParameters = new SBOParameters(args);
        this.sbmlValidation = args.getBooleanProperty(GeneralOptions.SBML_VALIDATION);
        this.outputType = IOOptions.OUTPUT_TYPE.parseOrCast(args.getProperty(IOOptions.OUTPUT_TYPE));
    }

    public PolishingParameters polishing() {
        return polishing;
    }

    public AnnotationParameters annotation() {
        return annotation;
    }

    public SBOParameters sboParameters() {
        return sboParameters;
    }

    public boolean sbmlValidation() {
        return sbmlValidation;
    }

    public IOOptions.OutputType outputType() {
        return outputType;
    }

    public FixingParameters fixing() {
        return fixing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameters that = (Parameters) o;
        return sbmlValidation == that.sbmlValidation && Objects.equals(fixing, that.fixing) && Objects.equals(polishing, that.polishing) && Objects.equals(annotation, that.annotation) && Objects.equals(sboParameters, that.sboParameters) && outputType == that.outputType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fixing, polishing, annotation, sboParameters, sbmlValidation, outputType);
    }

    @Override
    public String toString() {
        return "Parameters{" +
                "fixing=" + fixing +
                ", polishing=" + polishing +
                ", annotation=" + annotation +
                ", sboTerms=" + sboParameters +
                ", sbmlValidation=" + sbmlValidation +
                ", outputType=" + outputType +
                '}';
    }

}
