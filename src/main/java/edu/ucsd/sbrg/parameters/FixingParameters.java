package edu.ucsd.sbrg.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class FixingParameters {

    @JsonProperty("dont-fix")
    private boolean dontFix = false;

    public FixingParameters() {
    }

    public boolean dontFix() {
        return dontFix;
    }

    @Override
    public String toString() {
        return "FixingParameters{" +
                "dontFix=" + dontFix +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FixingParameters that = (FixingParameters) o;
        return dontFix == that.dontFix;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dontFix);
    }
}
