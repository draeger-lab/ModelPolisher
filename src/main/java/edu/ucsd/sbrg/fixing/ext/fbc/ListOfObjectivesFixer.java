package edu.ucsd.sbrg.fixing.ext.fbc;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.fixing.AbstractFixer;
import edu.ucsd.sbrg.fixing.IFixSBases;
import edu.ucsd.sbrg.logging.BundleNames;
import edu.ucsd.sbrg.parameters.FixingParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.util.ReactionNamePatterns;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.ListOfObjectives;
import org.sbml.jsbml.ext.fbc.Objective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class ListOfObjectivesFixer extends AbstractFixer implements IFixSBases<ListOfObjectives> {

    private static final Logger logger = LoggerFactory.getLogger(ListOfObjectivesFixer.class);
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.FIXING_MESSAGES);

    private final FBCModelPlugin modelPlug;
    private final FixingParameters fixingParameters;

    public ListOfObjectivesFixer(FixingParameters fixingParameters, FBCModelPlugin modelPlug, List<ProgressObserver> observers) {
        super(observers);
        this.modelPlug = modelPlug;
        this.fixingParameters = fixingParameters;
    }

    @Override
    public void fix(ListOfObjectives objectives, int index) {
        statusReport("Fixing Objectives (6/6)  ", objectives);

        var model = objectives.getModel();
        fixObjective(
                model.getId(),
                model.getListOfReactions(),
                modelPlug,
                fixingParameters.fluxObjectivesPolishingParameters().fluxCoefficients(),
                fixingParameters.fluxObjectivesPolishingParameters().fluxObjectives());

        // Identify and remove unused objectives, i.e., those without flux objectives
        Collection<Objective> removals = modelPlug.getListOfObjectives()
                .stream()
                .filter(Predicate.not(Objective::isSetListOfFluxObjectives)
                        .or(o -> o.getListOfFluxObjectives().isEmpty()))
                .toList();
        modelPlug.getListOfObjectives().removeAll(removals);
    }


    private void fixObjective(String modelDescriptor,
                                    ListOf<Reaction> listOfReactions,
                                    FBCModelPlugin fbcPlug,
                                    List<Double> fluxCoefficients,
                                    List<String> fluxObjectives) {
        Objective activeObjective = null;
        if (!fbcPlug.isSetActiveObjective()) {
            logger.debug(MessageFormat.format(MESSAGES.getString("OBJ_NOT_DEFINED"), modelDescriptor));
            if (fbcPlug.getObjectiveCount() == 1) {
                activeObjective = fbcPlug.getObjective(0);
                fbcPlug.setActiveObjective(activeObjective);
                logger.debug(MessageFormat.format(MESSAGES.getString("OBJ_SOLUTION"), activeObjective.getId()));
            }
        } else {
            activeObjective = fbcPlug.getListOfObjectives().getActiveObjectiveInstance();
        }
        if (activeObjective != null) {
            if (!activeObjective.isSetListOfFluxObjectives()) {
                logger.debug(MessageFormat.format(MESSAGES.getString("TRY_GUESS_MISSING_FLUX_OBJ"), modelDescriptor));
                if (listOfReactions != null) {
                    if (fluxObjectives != null && !fluxObjectives.isEmpty()) {
                        /*
                         * An array of target reactions is provided. We want to use this as
                         * flux objectives.
                         */
                        for (int i = 0; i < fluxObjectives.size(); i++) {
                            final String id = fluxObjectives.get(i);
                            Reaction r = listOfReactions.firstHit((obj) ->
                                    (obj instanceof Reaction) && id.equals(((Reaction) obj).getId()));
                            if (r != null) {
                                createFluxObjective(modelDescriptor, r, fluxCoefficients, activeObjective, i);
                            } else {
                                logger.debug(MessageFormat.format(MESSAGES.getString("REACTION_UNKNOWN_ERROR"), id, modelDescriptor));
                            }
                        }
                    } else {
                        /*
                         * Search for biomass reaction in the model and use this as
                         * objective.
                         */
                        final Pattern pattern = ReactionNamePatterns.BIOMASS_CASE_INSENSITIVE.getPattern();
                        Reaction rBiomass = listOfReactions.firstHit((obj) ->
                                (obj instanceof Reaction) && pattern.matcher(((Reaction) obj).getId()).matches());
                        if (rBiomass != null) {
                            createFluxObjective(modelDescriptor, rBiomass, fluxCoefficients, activeObjective, 0);
                        } else {
                            logger.debug(MESSAGES.getString("REACTION_BIOMASS_UNKNOWN_ERROR"));
                        }
                    }
                } else {
                    logger.debug(MessageFormat.format(MESSAGES.getString("REACTION_LIST_MISSING"), modelDescriptor));
                }
            }
        }
    }


    private void createFluxObjective(String modelDescriptor, Reaction r, List<Double> fluxCoefficients, Objective o,
                                            int i) {
        double defaultCoefficient = 1d;
        if ((fluxCoefficients != null) && (fluxCoefficients.size() > i)) {
            defaultCoefficient = fluxCoefficients.get(i);
        }
        logger.debug(MessageFormat.format(MESSAGES.getString("ADDED_FLUX_OBJ"), r.getId(), defaultCoefficient, modelDescriptor));
        o.createFluxObjective(null, null, defaultCoefficient, r);
    }
}
