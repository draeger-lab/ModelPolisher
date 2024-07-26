package edu.ucsd.sbrg.io;

import java.io.File;

public class ModelReaderException extends Exception {

    private final File input;

    public ModelReaderException(String s, Exception e, File input) {
        super(s, e);
        this.input = input;
    }

    public File input() {
        return input;
    }
}
