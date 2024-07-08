package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.BatchModeParameters;
import org.sbml.jsbml.SBMLDocument;

public class ModelAnnotator {

    public void annotate(SBMLDocument doc) {
        // Retrieve global parameters for the polishing process
        BatchModeParameters batchModeParameters = BatchModeParameters.get();
        // Annotate the document if the parameters specify
        if (batchModeParameters.annotateWithBiGG()) {
            BiGGAnnotation annotation = new BiGGAnnotation();
            annotation.annotate(doc);
        }
    }

}
