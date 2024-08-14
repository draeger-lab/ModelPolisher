package edu.ucsd.sbrg.fixing;

import org.sbml.jsbml.Compartment;

public class CompartmentFixer implements IFixSBases<Compartment> {

    @Override
    public void fix(Compartment compartment, int index) {
        // TODO: das ist nicht gut genug, was wenn es mehrere gibt?
        if (!compartment.isSetId()) {
            compartment.setId("d"); // default ID if none is set
        }

        // Ensure the compartment's 'constant' property is set to true if not already specified
        if (!compartment.isSetConstant()) {
            compartment.setConstant(true);
        }

    }
}
