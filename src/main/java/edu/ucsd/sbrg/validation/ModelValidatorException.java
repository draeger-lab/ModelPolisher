package edu.ucsd.sbrg.validation;

import java.io.File;

public class ModelValidatorException extends Exception {

    private final File outputFile;

    public ModelValidatorException(Exception e, File outputFile) {
        super(e);
        this.outputFile = outputFile;
    }

    public File outputFile() {
        return outputFile;
    }
}
