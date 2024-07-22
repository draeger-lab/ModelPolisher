package edu.ucsd.sbrg.annotation.adb;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.db.adb.AnnotateDB;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import org.sbml.jsbml.Reaction;

import java.util.*;

import static edu.ucsd.sbrg.db.adb.AnnotateDBContract.Constants.BIGG_REACTION;

public class ADBReactionsAnnotator extends AbstractADBAnnotator<Reaction> {

    public ADBReactionsAnnotator(AnnotateDB adb, Parameters parameters) {
        super(adb, parameters);
    }

    @Override
    public void annotate(List<Reaction> reactions) {
        reactions.forEach(this::annotate);
    }

    @Override
    public void annotate(Reaction reaction) {
        String id = reaction.getId();
        Optional<BiGGId> reactionId = BiGGId.createReactionId(id);
        reactionId.ifPresent(biGGId -> addBQB_IS_AnnotationsFromADB(reaction.getAnnotation(), BIGG_REACTION, biGGId));
        if ((reaction.getCVTermCount() > 0) && !reaction.isSetMetaId()) {
            reaction.setMetaId(reaction.getId());
        }
    }
}
