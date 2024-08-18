package edu.ucsd.sbrg.polishing;

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
