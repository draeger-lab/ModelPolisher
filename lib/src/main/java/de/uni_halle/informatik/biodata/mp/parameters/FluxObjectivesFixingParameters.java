package de.uni_halle.informatik.biodata.mp.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;
import de.uni_halle.informatik.biodata.mp.fixing.FixingOptions;

import java.util.ArrayList;
import java.util.List;

public class FluxObjectivesFixingParameters {

    @JsonProperty("flux-coefficients")
    protected List<Double> fluxCoefficients = List.of();
    @JsonProperty("flux-objectives")
    protected List<String> fluxObjectives = List.of(FixingOptions.FLUX_OBJECTIVES.getDefaultValue());

    public FluxObjectivesFixingParameters() {
    }

    public FluxObjectivesFixingParameters(List<Double> fluxCoefficients, List<String> fluxObjectives) {
        this.fluxCoefficients = fluxCoefficients;
        this.fluxObjectives = fluxObjectives.stream().map(String::trim).toList();
    }

    public FluxObjectivesFixingParameters(SBProperties args) {
        if (args.containsKey(FixingOptions.FLUX_COEFFICIENTS)) {
            String c = args.getProperty(FixingOptions.FLUX_COEFFICIENTS);
            String[] coeff = c.trim().split(",");
            fluxCoefficients = new ArrayList<>();
            for (String s : coeff) {
                fluxCoefficients.add(Double.parseDouble(s.trim()));
            }
        }
        if (args.containsKey(FixingOptions.FLUX_OBJECTIVES)) {
            String fObjectives = args.getProperty(FixingOptions.FLUX_OBJECTIVES);
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
