package edu.ucsd.sbrg.fixing.ext.fbc;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.util.ReactionNamePatterns;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.Objective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class ObjectiveFixer {

    private static final Logger logger = LoggerFactory.getLogger(ObjectiveFixer.class);
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
    private static final double DEFAULT_COEFFICIENT = 1d;

    public static void fixObjective(String modelDescriptor,
                                    ListOf<Reaction> listOfReactions,
                                    FBCModelPlugin fbcPlug,
                                    List<Double> fluxCoefficients,
                                    List<String> fluxObjectives) {
        Objective activeObjective = null;
        if (!fbcPlug.isSetActiveObjective()) {
            logger.info(MessageFormat.format(MESSAGES.getString("OBJ_NOT_DEFINED"), modelDescriptor));
            if (fbcPlug.getObjectiveCount() == 1) {
                activeObjective = fbcPlug.getObjective(0);
                fbcPlug.setActiveObjective(activeObjective);
                logger.info(MessageFormat.format(MESSAGES.getString("OBJ_SOLUTION"), activeObjective.getId()));
            }
        } else {
            activeObjective = fbcPlug.getListOfObjectives().getActiveObjectiveInstance();
        }
        if (activeObjective != null) {
            if (!activeObjective.isSetListOfFluxObjectives()) {
                logger.info(MessageFormat.format(MESSAGES.getString("TRY_GUESS_MISSING_FLUX_OBJ"), modelDescriptor));
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
                                logger.info(MessageFormat.format(MESSAGES.getString("REACTION_UNKNOWN_ERROR"), id, modelDescriptor));
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
                            logger.info(MESSAGES.getString("REACTION_BIOMASS_UNKNOWN_ERROR"));
                        }
                    }
                } else {
                    logger.info(MessageFormat.format(MESSAGES.getString("REACTION_LIST_MISSING"), modelDescriptor));
                }
            }
        }
    }


    private static void createFluxObjective(String modelDescriptor, Reaction r, List<Double> fluxCoefficients, Objective o,
                                            int i) {
        double coeff = DEFAULT_COEFFICIENT;
        if ((fluxCoefficients != null) && (fluxCoefficients.size() > i)) {
            coeff = fluxCoefficients.get(i);
        }
        logger.info(MessageFormat.format(MESSAGES.getString("ADDED_FLUX_OBJ"), r.getId(), coeff, modelDescriptor));
        o.createFluxObjective(null, null, coeff, r);
    }

}
