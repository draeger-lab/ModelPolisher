package edu.ucsd.sbrg.fixing;

import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Species;

import java.util.List;

public class CompartmentFixer extends AbstractFixer implements IFixSBases<Compartment> {

    protected CompartmentFixer(List<ProgressObserver> observers) {
        super(observers);
    }


    @Override
    public void fix(List<Compartment> rs) {
        statusReport("Fixing Compartments (4/6)  ", rs);
        IFixSBases.super.fix(rs);
    }


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
