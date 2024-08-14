package edu.ucsd.sbrg.fixing;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.util.ValuePair;

import static java.lang.String.format;

public class ReactionFixer implements IFixSBases<Reaction> {

    @Override
    public void fix(Reaction reaction, int index) {
        fixMissingReactionId(reaction, index);
        setFastProperty(reaction);
        setReversibleProperty(reaction);
    }

    private void fixMissingReactionId(Reaction reaction, int index) {
        if(null == reaction.getId() || reaction.getId().isEmpty()) {
            reaction.setId(format("reaction_without_id_%d", index));
        }
    }

    private void setReversibleProperty(Reaction reaction) {
        if (!reaction.isSetReversible()) {
            reaction.setReversible(false);
        }
    }

    @SuppressWarnings("deprecation")
    private void setFastProperty(Reaction reaction) {
        if ((!reaction.isSetLevelAndVersion()
                || reaction.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) <= 0)
                && !reaction.isSetFast()) {
            reaction.setFast(false);
        }
    }

}
