package de.uni_halle.informatik.biodata.mp.io;

import org.sbml.jsbml.SBMLDocument;

import java.io.File;

public class ModelWriterException extends Exception{

    private final SBMLDocument doc;
    private final File output;
    private final File archiveFile;

    public ModelWriterException(String s, Exception e, SBMLDocument doc, File output) {
        super(s, e);
        this.doc = doc;
        this.output = output;
        this.archiveFile = null;
    }

    public ModelWriterException(String s, SBMLDocument doc, File output, File archiveFile) {
        super(s);
        this.doc = doc;
        this.output = output;
        this.archiveFile = archiveFile;
    }

    public SBMLDocument doc() {
        return doc;
    }

    public File output() {
        return output;
    }

    public File archiveFile() {
        return archiveFile;
    }
}
