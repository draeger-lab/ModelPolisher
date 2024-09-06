package de.uni_halle.informatik.biodata.mp.annotation.adb;

import de.uni_halle.informatik.biodata.mp.annotation.IAnnotateSBases;
import de.uni_halle.informatik.biodata.mp.parameters.ADBAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDB;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import java.sql.SQLException;

public class ADBSBMLAnnotator extends AbstractADBAnnotator implements IAnnotateSBases<SBMLDocument> {

    public ADBSBMLAnnotator(AnnotateDB adb, ADBAnnotationParameters parameters) {
        super(adb, parameters);
    }

    @Override
    public void annotate(SBMLDocument doc) throws SQLException {
        Model model = doc.getModel();

        new ADBSpeciesAnnotator(adb, parameters).annotate(model.getListOfSpecies());
        new ADBReactionsAnnotator(adb, parameters).annotate(model.getListOfReactions());
    }
}
