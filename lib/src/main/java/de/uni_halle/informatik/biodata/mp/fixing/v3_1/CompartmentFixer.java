package de.uni_halle.informatik.biodata.mp.fixing.v3_1;

import de.uni_halle.informatik.biodata.mp.fixing.AbstractFixer;
import de.uni_halle.informatik.biodata.mp.fixing.IFixSBases;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.Compartment;

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
