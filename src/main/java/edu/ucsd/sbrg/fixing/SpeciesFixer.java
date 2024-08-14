package edu.ucsd.sbrg.fixing;

import org.sbml.jsbml.Species;

import static java.lang.String.format;

public class SpeciesFixer implements IFixSBases<Species> {

    @Override
    public void fix(Species species, int index) {
        if(null == species.getId() || species.getId().isEmpty()) {
            species.setId(format("species_without_id_%d", index));
        }

        // Set default values for mandatory attributes if they are not already set
        if (!species.isSetHasOnlySubstanceUnits()) {
            species.setHasOnlySubstanceUnits(true);
        }

        if (!species.isSetConstant()) {
            species.setConstant(false);
        }

        if (!species.isSetBoundaryCondition()) {
            species.setBoundaryCondition(false);
        }
    }

}
