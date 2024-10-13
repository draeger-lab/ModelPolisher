package de.uni_halle.informatik.biodata.mp.fixing;

import de.uni_halle.informatik.biodata.mp.fixing.ext.fbc.FBCSpeciesFixer;
import de.uni_halle.informatik.biodata.mp.fixing.ext.fbc.ListOfObjectivesFixer;
import de.uni_halle.informatik.biodata.mp.fixing.ext.groups.GroupsFixer;
import de.uni_halle.informatik.biodata.mp.parameters.FixingParameters;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;

import java.util.List;

public class ModelFixer extends AbstractFixer implements IFixSBases<Model> {

    private final FixingParameters fixingParameters;

    public ModelFixer(FixingParameters fixingParameters, List<ProgressObserver> observers) {
        super(observers);
        this.fixingParameters = fixingParameters;
    }

    @Override
    public void fix(Model model, int index) {
        statusReport("Fixing Model (1/6)  ", model);

        if(model.isSetListOfReactions()) {
            new ReactionFixer(getObservers()).fix(model.getListOfReactions());
        }

        if(model.isSetListOfSpecies()) {
            new SpeciesFixer(getObservers()).fix(model.getListOfSpecies());
        }

        if(model.isSetListOfCompartments()) {
            new CompartmentFixer(getObservers()).fix(model.getListOfCompartments());
        }

        if(model.isSetPlugin(GroupsConstants.shortLabel)) {
            GroupsModelPlugin plugin = (GroupsModelPlugin) model.getPlugin(GroupsConstants.shortLabel);
            new GroupsFixer(getObservers()).fix(plugin.getListOfGroups());
        }

        if(model.isSetPlugin(FBCConstants.shortLabel)) {
            FBCModelPlugin plugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
            new FBCSpeciesFixer().fix(model.getListOfSpecies()  );
            new ListOfObjectivesFixer(fixingParameters, plugin, getObservers()).fix(plugin.getListOfObjectives(), 0);
        }
    }
}
