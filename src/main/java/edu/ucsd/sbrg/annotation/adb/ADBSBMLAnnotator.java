package edu.ucsd.sbrg.annotation.adb;

import edu.ucsd.sbrg.parameters.ADBAnnotationParameters;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

public class ADBSBMLAnnotator extends AbstractADBAnnotator<SBMLDocument> {

    public ADBSBMLAnnotator(AnnotateDB adb, ADBAnnotationParameters parameters) {
        super(adb, parameters);
    }

    @Override
    public void annotate(SBMLDocument doc) {
        Model model = doc.getModel();

        new ADBSpeciesAnnotator(adb, parameters).annotate(model.getListOfSpecies());
        new ADBReactionsAnnotator(adb, parameters).annotate(model.getListOfReactions());
    }
}
