package de.uni_halle.informatik.biodata.mp.io;

import org.sbml.jsbml.SBMLDocument;

import java.io.File;

public interface IReadModelsFromFile {

    SBMLDocument read(File input) throws ModelReaderException;

}
