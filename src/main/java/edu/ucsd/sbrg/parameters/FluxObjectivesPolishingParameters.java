package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

import java.util.ArrayList;
import java.util.List;

public class FluxObjectivesPolishingParameters {

    public static final String FLUX_COEFFICIENTS = "fluxCoefficients";
    public static final String FLUX_OBJECTIVES = "fluxObjectives";

    @JsonProperty("flux-coefficients")
    protected List<Double> fluxCoefficients = List.of();
    @JsonProperty("flux-objectives")
    protected List<String> fluxObjectives = List.of(ModelPolisherOptions.FLUX_OBJECTIVES.getDefaultValue());

    public FluxObjectivesPolishingParameters() {
    }

    public FluxObjectivesPolishingParameters(List<Double> fluxCoefficients, List<String> fluxObjectives) {
        this.fluxCoefficients = fluxCoefficients;
        this.fluxObjectives = fluxObjectives.stream().map(String::trim).toList();
    }

    public FluxObjectivesPolishingParameters(SBProperties args) {
        if (args.containsKey(ModelPolisherOptions.FLUX_COEFFICIENTS)) {
            String c = args.getProperty(ModelPolisherOptions.FLUX_COEFFICIENTS);
            String[] coeff = c.trim().split(",");
            fluxCoefficients = new ArrayList<>();
            for (String s : coeff) {
                fluxCoefficients.add(Double.parseDouble(s.trim()));
            }
        }
        if (args.containsKey(ModelPolisherOptions.FLUX_OBJECTIVES)) {
            String fObjectives = args.getProperty(ModelPolisherOptions.FLUX_OBJECTIVES);
            fluxObjectives = List.of(fObjectives.trim().split(":"));
        }
    }

    public List<Double> fluxCoefficients() {
        return fluxCoefficients;
    }
    public List<String> fluxObjectives() {
        return fluxObjectives;
    }
}
