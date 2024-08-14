package edu.ucsd.sbrg.fixing.ext.fbc;

import edu.ucsd.sbrg.fixing.IFixSBases;
import edu.ucsd.sbrg.parameters.PolishingParameters;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.ListOfObjectives;
import org.sbml.jsbml.ext.fbc.Objective;

import java.util.Collection;
import java.util.function.Predicate;

public class ListOfObjectivesFixer implements IFixSBases<ListOfObjectives> {

    private final FBCModelPlugin modelPlug;
    private final PolishingParameters polishingParameters;

    public ListOfObjectivesFixer(PolishingParameters polishingParameters, FBCModelPlugin modelPlug) {
        this.modelPlug = modelPlug;
        this.polishingParameters = polishingParameters;
    }

    @Override
    public void fix(ListOfObjectives objectives, int index) {
        var model = objectives.getModel();
        ObjectiveFixer.fixObjective(
                model.getId(),
                model.getListOfReactions(),
                modelPlug,
                polishingParameters.fluxObjectivesPolishingParameters().fluxCoefficients(),
                polishingParameters.fluxObjectivesPolishingParameters().fluxObjectives());

        // Identify and remove unused objectives, i.e., those without flux objectives
        Collection<Objective> removals = modelPlug.getListOfObjectives()
                .stream()
                .filter(Predicate.not(Objective::isSetListOfFluxObjectives)
                        .or(o -> o.getListOfFluxObjectives().isEmpty()))
                .toList();
        modelPlug.getListOfObjectives().removeAll(removals);
    }
}
