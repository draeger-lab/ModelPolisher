package edu.ucsd.sbrg.polishing.fbc;

import edu.ucsd.sbrg.polishing.AbstractPolisher;
import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.util.SBMLFix;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.Objective;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class FluxObjectivesPolisher extends AbstractPolisher<Objective> {

    private final FBCModelPlugin modelPlug;

    public FluxObjectivesPolisher(FBCModelPlugin modelPlug, PolishingParameters parameters, Registry registry) {
        super(parameters, registry);
        this.modelPlug = modelPlug;
    }
    public FluxObjectivesPolisher(FBCModelPlugin modelPlug, PolishingParameters parameters, Registry registry, List<ProgressObserver>  observers) {
        super(parameters, registry, observers);
        this.modelPlug = modelPlug;
    }

    /**
     * Polishes the list of objectives in the given FBC model plugin.
     * This method checks for the presence of objectives and processes each one.
     * If no objectives are present, a warning is logged.
     * Each objective is checked for the presence of flux objectives, and if absent, attempts to fix them.
     * Objectives without any flux objectives are removed from the model.
     */
    @Override
    public void polish(List<Objective> objectives) {
        for (var objective : objectives) {
            statusReport("Polishing Objectives (7/9)  ", objective); // "Processing objective " + objective.getId());
            if (!objective.isSetListOfFluxObjectives() || objective.getListOfFluxObjectives().isEmpty()) {
                var model = objective.getModel();
                SBMLFix.fixObjective(
                        model.getId(),
                        model.getListOfReactions(),
                        modelPlug,
                        polishingParameters.fluxObjectivesPolishingParameters().fluxCoefficients(),
                        polishingParameters.fluxObjectivesPolishingParameters().fluxObjectives());
            }
        }
        // Identify and remove unused objectives, i.e., those without flux objectives
        Collection<Objective> removals = modelPlug.getListOfObjectives()
                .stream()
                .filter(Predicate.not(Objective::isSetListOfFluxObjectives)
                        .or(o -> o.getListOfFluxObjectives().isEmpty()))
                .toList();
        modelPlug.getListOfObjectives().removeAll(removals);
    }


    @Override
    public void polish(Objective elementToPolish) {
        throw new UnsupportedOperationException();
    }


    // TODO: this logging was pointless but we need to provide this information
//    /**
//     * Polishes the list of flux objectives within a given objective.
//     * This method checks for the presence and validity of flux objectives and logs warnings if:
//     * - No flux objectives are present.
//     * - There are more than one flux objectives.
//     * - Flux objectives have invalid coefficients.
//     *
//     * @param objective The objective whose flux objectives are to be polished.
//     */
//    private void polishListOfFluxObjectives(Objective objective) {
//        if (objective.getFluxObjectiveCount() == 0) {
//            // Note: the strict attribute does not require the presence of any flux objectives.
//            logger.warning(format(MESSAGES.getString("OBJ_FLUX_OBJ_MISSING"), objective.getId()));
//        } else {
//            if (objective.getFluxObjectiveCount() > 1) {
//                logger.warning(format(MESSAGES.getString("TOO_MUCH_OBJ_TARGETS"), objective.getId()));
//            }
//            for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()) {
//                if (!fluxObjective.isSetCoefficient() || Double.isNaN(fluxObjective.getCoefficient())
//                        || !Double.isFinite(fluxObjective.getCoefficient())) {
//                    logger.warning(format(MESSAGES.getString("FLUX_OBJ_COEFF_INVALID"), fluxObjective.getReaction()));
//                }
//            }
//        }
//    }

}
