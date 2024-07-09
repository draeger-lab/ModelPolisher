package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.Parameters;
import org.sbml.jsbml.SBMLDocument;

public class ModelAnnotator {

    private final Parameters params;

    public ModelAnnotator() {
        this.params = Parameters.get();
    }

    public void annotate(SBMLDocument doc) {
        // Annotate the document if the parameters specify
        if (params.annotateWithBiGG()) {
            BiGGAnnotation annotation = new BiGGAnnotation();
            annotation.annotate(doc);
        }
    }

}
