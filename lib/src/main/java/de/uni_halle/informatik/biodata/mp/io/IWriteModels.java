package de.uni_halle.informatik.biodata.mp.io;

import org.sbml.jsbml.SBMLDocument;

import java.io.InputStream;

public interface IWriteModels {

    InputStream write(SBMLDocument doc) throws ModelWriterException;

}
