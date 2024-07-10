package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.Parameters;
import org.sbml.jsbml.SBMLDocument;

public class ModelAnnotator {

    private final Parameters parameters;

    public ModelAnnotator(Parameters parameters) {
        this.parameters = parameters;
    }

    public void annotate(SBMLDocument doc) {
        // Annotate the document if the parameters specify
        if (parameters.annotateWithBiGG()) {
            BiGGAnnotation annotation = new BiGGAnnotation(parameters);
            annotation.annotate(doc);
        }
    }

}
