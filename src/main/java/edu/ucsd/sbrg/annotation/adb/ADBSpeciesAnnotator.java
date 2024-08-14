package edu.ucsd.sbrg.annotation.adb;

import edu.ucsd.sbrg.annotation.IAnnotateSBases;
import edu.ucsd.sbrg.parameters.ADBAnnotationParameters;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import org.sbml.jsbml.Species;

import java.sql.SQLException;
import java.util.*;

import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.BIGG_METABOLITE;

public class ADBSpeciesAnnotator extends AbstractADBAnnotator implements IAnnotateSBases<Species> {

    public ADBSpeciesAnnotator(AnnotateDB adb, ADBAnnotationParameters parameters) {
        super(adb, parameters);
    }

    @Override
    public void annotate(List<Species> species) throws SQLException {
        for (Species s : species) {
            annotate(s);
        }
    }

    @Override
    public void annotate(Species species) throws SQLException {
        String id = species.getId();
        var metaboliteId = BiGGId.createMetaboliteId(id);
        addBQB_IS_AnnotationsFromADB(species.getAnnotation(), BIGG_METABOLITE, metaboliteId);
        if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
            species.setMetaId(species.getId());
        }
    }
}
