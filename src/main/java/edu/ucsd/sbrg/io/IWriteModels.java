package edu.ucsd.sbrg.io;

import org.sbml.jsbml.SBMLDocument;

import java.io.InputStream;

public interface IWriteModels {

    InputStream write(SBMLDocument doc) throws ModelWriterException;

}
