package edu.ucsd.sbrg.annotation.adb;

import edu.ucsd.sbrg.parameters.ADBAnnotationParameters;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import org.sbml.jsbml.Species;

import java.util.*;

import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.BIGG_METABOLITE;

public class ADBSpeciesAnnotator extends AbstractADBAnnotator<Species> {

    public ADBSpeciesAnnotator(AnnotateDB adb, ADBAnnotationParameters parameters) {
        super(adb, parameters);
    }

    @Override
    public void annotate(List<Species> species) {
        species.forEach(this::annotate);
    }

    @Override
    public void annotate(Species species) {
        String id = species.getId();
        Optional<BiGGId> metaboliteId = BiGGId.createMetaboliteId(id);
        metaboliteId.ifPresent(biGGId -> addBQB_IS_AnnotationsFromADB(species.getAnnotation(), BIGG_METABOLITE, biGGId));
        if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
            species.setMetaId(species.getId());
        }
    }
}
