package de.uni_halle.informatik.biodata.mp.io;

import org.sbml.jsbml.SBMLDocument;

import java.io.File;

public interface IWriteModelsToFile {

    File write(SBMLDocument doc, File output) throws ModelWriterException;
}
