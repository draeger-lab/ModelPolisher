package de.uni_halle.informatik.biodata.mp.annotation.adb;

import de.uni_halle.informatik.biodata.mp.annotation.IAnnotateSBases;
import de.uni_halle.informatik.biodata.mp.parameters.ADBAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDB;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import org.sbml.jsbml.Reaction;

import java.sql.SQLException;
import java.util.*;

import static de.uni_halle.informatik.biodata.mp.db.adb.AnnotateDBContract.Constants.BIGG_REACTION;

public class ADBReactionsAnnotator extends AbstractADBAnnotator implements IAnnotateSBases<Reaction> {

    public ADBReactionsAnnotator(AnnotateDB adb, ADBAnnotationParameters parameters) {
        super(adb, parameters);
    }

    @Override
    public void annotate(List<Reaction> reactions) throws SQLException {
        for (Reaction reaction : reactions) {
            annotate(reaction);
        }
    }

    @Override
    public void annotate(Reaction reaction) throws SQLException {
        String id = reaction.getId();
        var reactionId = BiGGId.createReactionId(id);
        addBQB_IS_AnnotationsFromADB(reaction.getAnnotation(), BIGG_REACTION, reactionId);
        if ((reaction.getCVTermCount() > 0) && !reaction.isSetMetaId()) {
            reaction.setMetaId(reaction.getId());
        }
    }
}
