package edu.ucsd.sbrg.polishing.fbc;

import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

/**
 * From <a href="https://sbml.org/software/libsbml/5.18.0/docs/formatted/java-api/org/sbml/libsbml/FbcModelPlugin.html">...</a>:
 * <p>
 *  The following restrictions are in effect if an “fbc” model object has a value of 'true'
 *  for the attribute 'strict' on Model:
 *  1) Each Reaction in a Model must define values for the attributes 'lowerFluxBound' and 'upperFluxBound',
 *      with each attribute pointing to a valid Parameter object defined in the current Model.
 *      (implemented via {@link #reactionHasValidBounds})
 *  2) Each Parameter object referred to by the Reaction attributes 'lowerFluxBound' and 'upperFluxBound'
 *      must have its 'constant' attribute set to the value 'true'
 *      and its 'value' attribute set to a value of type double. This value may not be 'NaN'.
 *      (implemented via {@link #reactionHasValidBounds})
 *  3) SpeciesReference objects in Reaction objects must have their 'stoichiometry' attribute set to a double value
 *      that is not 'NaN', nor '-INF', nor 'INF'.
 *      In addition, the value of their 'constant' attribute must be set to 'true'.
 *      (implemented via {@link #reactionSpeciesReferencesHaveValidAttributes})
 *  4) InitialAssignment objects may not target the Parameter objects referenced by
 *      the Reaction attributes 'lowerFluxBound' and 'upperFluxBound', nor any SpeciesReference objects.
 *      (implemented via {@link #initialAssignmentDoesNotReferenceBoundParameters}
 *      and {@link #initialAssignmentDoesNotReferenceSpeciesReferences})
 *  5) All defined FluxObjective objects must have their coefficient attribute set
 *      to a double value that is not 'NaN', nor '-INF', nor 'INF'.
 *      (implemented via {@link #fluxObjectiveHasValidCoefficients})
 *  6) A Reaction 'lowerFluxBound' attribute may not point to a Parameter object that has a value of 'INF'.
 *      (implemented via {@link #reactionHasValidBounds})
 *  7) A Reaction 'upperFluxBound' attribute may not point to a Parameter object that has a value of '-INF'.
 *      (implemented via {@link #reactionHasValidBounds})
 *  8) For all Reaction objects, the value of a 'lowerFluxBound' attribute must be
 *      less than or equal to the value of the 'upperFluxBound' attribute.
 *      (implemented via {@link #reactionHasValidBounds})
 */
public class StrictnessPredicate implements Predicate<Model> {
    private static final Logger logger = LoggerFactory.getLogger(StrictnessPredicate.class);

    @Override
    public boolean test(Model model) {
        logger.debug("Test Strictness");

        var strict = model.getListOfReactions().stream()
                .allMatch(reaction ->
                        reactionHasValidBounds(reaction)
                                && reactionSpeciesReferencesHaveValidAttributes(reaction));

        if (model.isSetListOfInitialAssignments()){
            strict &= model.getListOfInitialAssignments().stream()
                    .allMatch(ia ->
                            initialAssignmentDoesNotReferenceBoundParameters(ia)
                                    && initialAssignmentDoesNotReferenceSpeciesReferences(ia));

        }

        FBCModelPlugin modelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        if (modelPlug.isSetListOfObjectives()) {
            strict &= modelPlug.getListOfObjectives().stream()
                    .map(Objective::getListOfFluxObjectives)
                    .flatMap(List::stream)
                    .allMatch(this::fluxObjectiveHasValidCoefficients);
        }

        return strict;
    }

    /**
     * 1) Each Reaction in a Model must define values for the attributes 'lowerFluxBound' and 'upperFluxBound',
     *     with each attribute pointing to a valid Parameter object defined in the current Model.
     * 2) Each Parameter object referred to by the Reaction attributes 'lowerFluxBound' and 'upperFluxBound'
     *     must have its 'constant' attribute set to the value 'true'
     *     and its 'value' attribute set to a value of type double. This value may not be 'NaN'.
     * 6) A Reaction 'lowerFluxBound' attribute may not point to a Parameter object that has a value of 'INF'.
     * 7) A Reaction 'upperFluxBound' attribute may not point to a Parameter object that has a value of '-INF'.
     * 8) For all Reaction objects, the value of a 'lowerFluxBound' attribute must be
     *     less than or equal to the value of the 'upperFluxBound' attribute.
     */
    public boolean reactionHasValidBounds(Reaction r) {
        FBCReactionPlugin rPlug = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

        Parameter lb = rPlug.getLowerFluxBoundInstance();
        Parameter ub = rPlug.getUpperFluxBoundInstance();

        // TODO: pointless logging but we should provide this information
//        if (!strict) {
//            logger.warning(format(MESSAGES.getString("FLUX_BOUND_ERROR"), reaction.getId()));
//        }

        return isBoundSet(lb)
                && lb.isConstant()
                && lb.getValue() < Double.POSITIVE_INFINITY
                && isBoundSet(ub)
                && ub.isConstant()
                && ub.getValue() > Double.NEGATIVE_INFINITY
                && lb.getValue() <= ub.getValue();
    }

    public boolean isBoundSet(Parameter bound) {
        return (bound != null)
                && bound.isSetValue()
                && !Double.isNaN(bound.getValue());
    }

    /**
     * see {@link #strictnessOfSpeciesReferences}
     */
    public boolean reactionSpeciesReferencesHaveValidAttributes(Reaction r) {
        return strictnessOfSpeciesReferences(r.getListOfReactants())
                && strictnessOfSpeciesReferences(r.getListOfProducts());
    }

    /**
     * 3) SpeciesReference objects in Reaction objects must have their 'stoichiometry' attribute set to a double value
     *      that is not 'NaN', nor '-INF', nor 'INF'.
     *      In addition, the value of their 'constant' attribute must be set to 'true'.
     */
    public boolean strictnessOfSpeciesReferences(ListOf<SpeciesReference> listOfSpeciesReference) {
        boolean strict = true;
        for (SpeciesReference sr : listOfSpeciesReference) {
            strict &= sr.isConstant()
                    && sr.isSetStoichiometry()
                    && !Double.isNaN(sr.getValue())
                    && Double.isFinite(sr.getValue());
        }
        return strict;
    }

    /**
     * 4) InitialAssignment objects may not target the Parameter objects referenced by
     *     the Reaction attributes 'lowerFluxBound' and 'upperFluxBound', nor any SpeciesReference objects.
     */
    public boolean initialAssignmentDoesNotReferenceBoundParameters(InitialAssignment ia) {
        var variable = ia.getVariableInstance();
        // TODO: this is a suboptimal implementation as it relies on SBO terms instead of inspecting the model
        return  (variable instanceof Parameter
                && variable.isSetSBOTerm() && SBO.isChildOf(variable.getSBOTerm(), 625));
    }

    /**
     * 4) InitialAssignment objects may not target the Parameter objects referenced by
     *     the Reaction attributes 'lowerFluxBound' and 'upperFluxBound', nor any SpeciesReference objects.
     */
    public boolean initialAssignmentDoesNotReferenceSpeciesReferences(InitialAssignment ia) {
        var variable = ia.getVariableInstance();
        return !(variable instanceof SpeciesReference);
    }

    /**
     * 5) All defined FluxObjective objects must have their coefficient attribute set
     *     to a double value that is not 'NaN', nor '-INF', nor 'INF'.
     */
    public Boolean fluxObjectiveHasValidCoefficients(FluxObjective fo) {
        return !Double.isNaN(fo.getCoefficient())
                && Double.isFinite(fo.getCoefficient());
    }
}
