package de.uni_halle.informatik.biodata.mp.annotation.adb;

import de.uni_halle.informatik.biodata.mp.annotation.IAnnotateSBases;
import de.uni_halle.informatik.biodata.mp.parameters.ADBAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDB;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import org.sbml.jsbml.Species;

import java.sql.SQLException;
import java.util.*;

import static de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDBContract.Constants.BIGG_METABOLITE;

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
