package edu.ucsd.sbrg.polishing.ext.fbc;

import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.polishing.AbstractPolisher;
import edu.ucsd.sbrg.polishing.IPolishSBases;
import edu.ucsd.sbrg.util.ReactionNamePatterns;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.util.ext.fbc.GPRParser;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.xml.XMLNode;

import java.util.List;
import java.util.Optional;

public class FBCReactionPolisher extends AbstractPolisher implements IPolishSBases<Reaction> {
    private final FBCModelPlugin fbcPlugin;
    private final SBOParameters sboParameters;
    private final GeneProductAssociationsProcessor gpaProcessor;

    public FBCReactionPolisher(FBCModelPlugin fbcPlugin,
                               PolishingParameters polishingParameters,
                               SBOParameters sboParameters,
                               Registry registry) {
        super(polishingParameters, registry);
        this.fbcPlugin = fbcPlugin;
        this.sboParameters = sboParameters;
        this.gpaProcessor = new GeneProductAssociationsProcessor();
    }

    public FBCReactionPolisher(FBCModelPlugin fbcPlugin,
                               PolishingParameters polishingParameters,
                               SBOParameters sboParameters,
                               Registry registry,
                               List<ProgressObserver> observers) {
        super(polishingParameters, registry, observers);
        this.fbcPlugin = fbcPlugin;
        this.sboParameters = sboParameters;
        this.gpaProcessor = new GeneProductAssociationsProcessor();
    }


    @Override
    public void polish(List<Reaction> reactions) {
        for (var reaction : reactions) {
            polish(reaction);
        }
    }

    @Override
    public void polish(Reaction reaction) {
        if (fbcPlugin.isSetListOfObjectives()) {
            fluxObjectiveFromLocalParameter(reaction);
        }
        associationFromNotes(reaction);
        polishBounds(reaction);
        gpaProcessor.convertAssociationsToFBCV2(reaction, sboParameters.addGenericTerms());
    }


    private void fluxObjectiveFromLocalParameter(Reaction reaction) {
        Objective obj = fbcPlugin.getActiveObjectiveInstance();

        // Check if a flux objective associated with the reaction already exists
        boolean foExists = obj.getListOfFluxObjectives().stream()
                .anyMatch(fo -> fo.getReactionInstance().equals(reaction));
        if (foExists) {
            return;
        }
        // Retrieve the kinetic law of the reaction, if it exists
        KineticLaw kl = reaction.getKineticLaw();
        if (kl != null) {
            // Attempt to get the objective coefficient from the kinetic law
            LocalParameter coefficient = kl.getLocalParameter("OBJECTIVE_COEFFICIENT");
            if (coefficient != null && coefficient.getValue() != 0d) {
                // Create a new flux objective with the coefficient and associate it with the reaction
                FluxObjective fo = obj.createFluxObjective("fo_" + reaction.getId());
                fo.setCoefficient(coefficient.getValue());
                fo.setReaction(reaction);
            }
        }
    }


    private void associationFromNotes(Reaction reaction) {
        // Obtain the FBC plugin for the reaction to handle FBC-specific features.
        var reactionPlugin = (FBCReactionPlugin) reaction.getPlugin(FBCConstants.shortLabel);

        // Check if the gene product association is not already set and if the reaction has notes.
        if (!reactionPlugin.isSetGeneProductAssociation() && reaction.isSetNotes()) {
            // Retrieve the 'body' element from the reaction notes.
            XMLNode body = reaction.getNotes().getChildElement("body", null);

            // Process each paragraph within the body that contains exactly one child node.
            if (body != null) {
                for (XMLNode p : body.getChildElements("p", null)) {
                    if (p.getChildCount() == 1) {
                        String associationCandidate = p.getChildAt(0).getCharacters();

                        // Check if the text starts with the expected gene association tag.
                        if (associationCandidate.startsWith("GENE_ASSOCIATION: ")) {
                            String[] splits = associationCandidate.split("GENE_ASSOCIATION: ");

                            // Ensure the string was split into exactly two parts and the second part is not empty.
                            if (splits.length == 2) {
                                String association = splits[1];
                                if (!association.isEmpty()) {
                                    // Parse the gene product association and apply it to the reaction.
                                    GPRParser.setGeneProductAssociation(reaction, association, sboParameters.addGenericTerms());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the existing FBC flux bounds are strictly defined and attempts to infer missing bounds from the reaction's kinetic law.
     * If bounds are not set, it creates and assigns new global parameters as flux bounds according to the FBC specification.
     */
    private void polishBounds(Reaction reaction) {
        // TODO: this code does multiple unrelated things at once; check for strictness should be its own function
        FBCReactionPlugin rPlug = (FBCReactionPlugin) reaction.getPlugin(FBCConstants.shortLabel);

        Parameter lb = rPlug.getLowerFluxBoundInstance();
        Parameter ub = rPlug.getUpperFluxBoundInstance();

        // try to set bounds if none exist yet
        if (lb == null) {
            lb = ensureBound(reaction, "LOWER_BOUND");
        }
        if (ub == null) {
            ub = ensureBound(reaction, "UPPER_BOUND");
        }

        // set appropriate SBO terms for bounds
        if (lb != null) {
            setFluxBoundSBOTerm(rPlug.getLowerFluxBoundInstance());
        }
        if (ub != null) {
            setFluxBoundSBOTerm(rPlug.getUpperFluxBoundInstance());
        }
    }


    private Parameter ensureBound(Reaction reaction, String boundType) {
        // set bounds from KineticLaw, if they are not set in FBC, create global Parameter,
        // as required by specification
        Parameter bound = getBoundFromKineticLawParameters(reaction, boundType);

        if (bound != null) {
            setBoundId(reaction, bound, bound.getValue());
            var preexistingParameter = reaction.getModel().getParameter(bound.getId());
            if (preexistingParameter == null) {
                reaction.getModel().addParameter(bound);
                updateReactionPlugin(reaction, boundType, bound);
            } else {
                updateReactionPlugin(reaction, boundType, preexistingParameter);
            }
        }
        return bound;
    }

    private void updateReactionPlugin(Reaction reaction, String boundType, Parameter parameter) {
        FBCReactionPlugin rPlug = (FBCReactionPlugin) reaction.getPlugin(FBCConstants.shortLabel);
        if (boundType.equals("LOWER_BOUND")) {
            rPlug.setLowerFluxBound(parameter);
        } else if (boundType.equals("UPPER_BOUND")) {
            rPlug.setUpperFluxBound(parameter);
        }
    }


    /**
     * Polishes the SBO term of a flux bound parameter based on its ID.
     * If the parameter's ID matches the default flux bound pattern, it sets the SBO term to 626.
     * Otherwise, it sets the SBO term to 625.
     *
     * @param bound The parameter representing a flux bound.
     */
    public void setFluxBoundSBOTerm(Parameter bound) {
        if (ReactionNamePatterns.DEFAULT_FLUX_BOUND.getPattern().matcher(bound.getId()).matches()) {
            bound.setSBOTerm(626); // default flux bound
        } else {
            bound.setSBOTerm(625); // flux bound
        }
    }

    /**
     * Retrieves a local parameter from a reaction's kinetic law based on the specified parameter name.
     * This method specifically looks for parameters that define either the lower or upper flux bounds.
     *
     * @param r             The reaction from which the kinetic law and the parameter are to be retrieved.
     * @param parameterName The name of the parameter to retrieve, expected to be either "LOWER_BOUND" or "UPPER_BOUND".
     * @return The local parameter if found, or {@code null} if the kinetic law is not defined or the parameter does not exist.
     */
    private Parameter getBoundFromKineticLawParameters(Reaction r, String parameterName) {
        return Optional.ofNullable(r.getKineticLaw())
                .map(kl -> kl.getLocalParameter(parameterName))
                .map(Parameter::new)
                .orElse(null);
    }


    /**
     * Retrieves a modified {@link Parameter} instance based on the specified bound value.
     * This method adjusts the ID of the {@link Parameter} based on predefined threshold values.
     * If the bound value matches a specific threshold, the ID is set to a corresponding default value.
     * Otherwise, the ID is customized using the reaction's ID combined with the original bound's ID.
     *
     * @param r          The {@link Reaction} instance from which the model and parameter are derived.
     * @param bound      The {@link Parameter} instance representing either a lower or upper bound.
     * @param boundValue The numeric value of the bound, which determines how the {@link Parameter}'s ID is set.
     */
    private void setBoundId(Reaction r, Parameter bound, double boundValue) {
        if (boundValue == -1000d) {
            bound.setId("DEFAULT_LOWER_BOUND");
        } else if (boundValue == 0d) {
            bound.setId("DEFAULT_BOUND");
        } else if (boundValue == 1000d) {
            bound.setId("DEFAULT_UPPER_BOUND");
        } else {
            bound.setId(r.getId() + "_" + bound.getId());
        }
    }

}
