package edu.ucsd.sbrg.annotation.adb;

import edu.ucsd.sbrg.parameters.ADBAnnotationParameters;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import org.sbml.jsbml.Reaction;

import java.sql.SQLException;
import java.util.*;

import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.BIGG_REACTION;

public class ADBReactionsAnnotator extends AbstractADBAnnotator<Reaction> {

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
        Optional<BiGGId> reactionId = BiGGId.createReactionId(id);
        if (reactionId.isPresent()) {
            addBQB_IS_AnnotationsFromADB(reaction.getAnnotation(), BIGG_REACTION, reactionId.get());
        }
        if ((reaction.getCVTermCount() > 0) && !reaction.isSetMetaId()) {
            reaction.setMetaId(reaction.getId());
        }
    }
}
