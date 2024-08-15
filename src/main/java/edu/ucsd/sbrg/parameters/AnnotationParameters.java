package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.zbit.util.prefs.SBProperties;

import java.util.Objects;

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

    @Override
    public String toString() {
        return "AnnotationParameters{" +
                "adbAnnotationParameters=" + adbAnnotationParameters +
                ", biGGAnnotationParameters=" + biGGAnnotationParameters +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotationParameters that = (AnnotationParameters) o;
        return Objects.equals(adbAnnotationParameters, that.adbAnnotationParameters) && Objects.equals(biGGAnnotationParameters, that.biGGAnnotationParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(adbAnnotationParameters, biGGAnnotationParameters);
    }

}
