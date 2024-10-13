package de.uni_halle.informatik.biodata.mp.fixing;

import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.Species;

import java.util.List;

import static java.lang.String.format;

public class SpeciesFixer extends AbstractFixer implements IFixSBases<Species> {

    protected SpeciesFixer(List<ProgressObserver> observers) {
        super(observers);
    }

    @Override
    public void fix(List<Species> rs) {
        statusReport("Fixing Species (3/6)  ", rs);
        IFixSBases.super.fix(rs);
    }


    @Override
    public void fix(Species species, int index) {
        if(null == species.getId() || species.getId().isEmpty()) {
            species.setId(format("species_without_id_%d", index));
        }

        // Set default values for mandatory attributes if they are not already set
        if (!species.isSetHasOnlySubstanceUnits()) {
            species.setHasOnlySubstanceUnits(true);
        }

        if (!species.isSetConstant() && species.getLevel() >= 3) {
            species.setConstant(false);
        }

        if (!species.isSetBoundaryCondition()) {
            species.setBoundaryCondition(false);
        }
    }

}
