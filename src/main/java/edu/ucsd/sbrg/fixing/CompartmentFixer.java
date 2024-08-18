package edu.ucsd.sbrg.fixing;

import org.sbml.jsbml.Compartment;

public class CompartmentFixer implements IFixSBases<Compartment> {

    @Override
    public void fix(Compartment compartment, int index) {
        if (!compartment.isSetId()) {
            compartment.setId("default_id_" + index); // default ID if none is set
        }

        // Ensure the compartment's 'constant' property is set to true if not already specified
        if (!compartment.isSetConstant()) {
            compartment.setConstant(true);
        }

    }
}
