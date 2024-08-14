package edu.ucsd.sbrg.io;

import org.sbml.jsbml.SBMLDocument;

import java.io.File;

public interface IReadModelsFromFile {

    SBMLDocument read(File input) throws ModelReaderException;

}
