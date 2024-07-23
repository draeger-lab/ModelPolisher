package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

public class AnnotationParameters {

    @JsonProperty("annotatedb")
    private ADBAnnotationParameters adbAnnotationParameters = new ADBAnnotationParameters();
    @JsonProperty("bigg")
    private BiGGAnnotationParameters biGGAnnotationParameters = new BiGGAnnotationParameters();

    public AnnotationParameters() {
    }

    public AnnotationParameters(SBProperties args) {
        biGGAnnotationParameters = new BiGGAnnotationParameters(args);
        adbAnnotationParameters = new ADBAnnotationParameters(args);
    }

    public ADBAnnotationParameters adbAnnotationParameters() {
        return adbAnnotationParameters;
    }

    public BiGGAnnotationParameters biggAnnotationParameters() {
        return biGGAnnotationParameters;
    }
}
