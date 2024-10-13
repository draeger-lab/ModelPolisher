package de.uni_halle.informatik.biodata.mp.polishing.ext.fbc;

import de.uni_halle.informatik.biodata.mp.polishing.AbstractPolisher;
import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.polishing.AnnotationPolisher;
import de.uni_halle.informatik.biodata.mp.polishing.IPolishSBases;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.Objective;

import java.util.List;

public class ObjectivesPolisher extends AbstractPolisher implements IPolishSBases<Objective> {

    private FBCModelPlugin fbcPlugin;

    public ObjectivesPolisher(FBCModelPlugin fbcPlugin, PolishingParameters parameters, Registry registry) {
        super(parameters, registry);
        this.fbcPlugin = fbcPlugin;
    }
    public ObjectivesPolisher(FBCModelPlugin fbcPlugin, PolishingParameters parameters, Registry registry, List<ProgressObserver>  observers) {
        super(parameters, registry, observers);
        this.fbcPlugin = fbcPlugin;
    }

    @Override
    public void polish(Objective objective) {
        statusReport("Polishing Objectives (7/9)  ", objective);

        new AnnotationPolisher(polishingParameters, registry).polish(objective.getAnnotation());

        if ((objective.getCVTermCount() > 0) && !objective.isSetMetaId()) {
            objective.setMetaId(objective.getId());
        }

        createDefaultObjective();
    }

    private void createDefaultObjective() {
        if (!fbcPlugin.isSetListOfObjectives()) {
            var obj = fbcPlugin.createObjective("obj", "default objective", Objective.Type.MAXIMIZE);
            fbcPlugin.getListOfObjectives().setActiveObjective(obj.getId());
        }
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
