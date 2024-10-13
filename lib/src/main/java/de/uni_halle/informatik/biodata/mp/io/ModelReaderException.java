package de.uni_halle.informatik.biodata.mp.io;

import java.io.File;

public class ModelReaderException extends Throwable {

    private final File input;

    public ModelReaderException(String s, File input) {
        super(s);
        this.input = input;
    }

    public ModelReaderException(String s, Exception e, File input) {
        super(s, e);
        this.input = input;
    }

    public File input() {
        return input;
    }
}
