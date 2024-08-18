package edu.ucsd.sbrg.fixing;

import org.sbml.jsbml.Model;

public class ModelFixer implements IFixSBases<Model> {

    @Override
    public void fix(Model model, int index) {
        if(model.isSetListOfReactions()) {
            new ReactionFixer().fix(model.getListOfReactions());
        }
        if(model.isSetListOfSpecies()) {
            new SpeciesFixer().fix(model.getListOfSpecies());
        }
        if(model.isSetListOfCompartments()) {
            new CompartmentFixer().fix(model.getListOfCompartments());
        }
    }
}
