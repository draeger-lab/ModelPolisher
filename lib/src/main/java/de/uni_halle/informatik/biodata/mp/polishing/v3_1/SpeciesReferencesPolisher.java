package de.uni_halle.informatik.biodata.mp.polishing.v3_1;

import de.uni_halle.informatik.biodata.mp.polishing.IPolishSpeciesReferences;
import org.sbml.jsbml.SpeciesReference;

public class SpeciesReferencesPolisher implements IPolishSpeciesReferences {

    private final Integer defaultSBOterm;

    public SpeciesReferencesPolisher(Integer defaultSBOterm) {
        this.defaultSBOterm = defaultSBOterm;
    }

    @Override
    public void polish(SpeciesReference sr) {
        if (!sr.isSetSBOTerm() && defaultSBOterm != null) {
            sr.setSBOTerm(defaultSBOterm);
        }
    }
}
