package de.uni_halle.informatik.biodata.mp.fixing;

import org.sbml.jsbml.SpeciesReference;

public class SpeciesReferenceFixer implements IFixSpeciesReferences {

    @Override
    public void fix(SpeciesReference sr) {
        if (sr.getSpeciesInstance() == null) {
            // TODO: das ist nicht recoverable
        }
        if (!sr.isSetConstant() && sr.getLevel() >= 3) {
            sr.setConstant(false);
        }
    }
}
