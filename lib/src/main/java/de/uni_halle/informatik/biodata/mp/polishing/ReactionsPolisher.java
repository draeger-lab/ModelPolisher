    package de.uni_halle.informatik.biodata.mp.polishing;

    import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
    import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
    import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
    import de.uni_halle.informatik.biodata.mp.resolver.Registry;
    import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
    import de.uni_halle.informatik.biodata.mp.util.ReactionNamePatterns;
    import org.sbml.jsbml.*;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import java.util.List;
    import java.util.Objects;
    import java.util.Optional;
    import java.util.stream.Collectors;


    /**
     * This class provides methods to polish and validate SBML reactions according to specific rules and patterns.
     * It includes functionality to:
     * - Check and set SBO terms based on reaction ID patterns.
     * - Polish species references and compartments.
     * - Validate and set flux bounds and objectives.
     * - Convert gene associations from reaction notes to FBCv2 format.
     * - Check mass and atom balance of reactions.
     * <p>
     * The class operates on an SBML {@link Reaction} object and modifies it to conform to standards and conventions
     * used in systems biology models, particularly those related to flux balance constraints.
     */
    public class ReactionsPolisher extends AbstractPolisher implements IPolishSBases<Reaction> {

        private static final Logger logger = LoggerFactory.getLogger(ReactionsPolisher.class);

        private final SBOParameters sboParameters;

        public ReactionsPolisher(PolishingParameters polishingParameters,
                                 SBOParameters sboParameters,
                                 Registry registry) {
            super(polishingParameters, registry);
            this.sboParameters = sboParameters;
        }

        public ReactionsPolisher(PolishingParameters polishingParameters,
                                 SBOParameters sboParameters,
                                 Registry registry,
                                 List<ProgressObserver> observers) {
            super(polishingParameters, registry, observers);
            this.sboParameters = sboParameters;
        }

        @Override
        public void polish(List<Reaction> reactions) {
            logger.debug("Polish Reactions");

            for (var reaction : reactions) {
                statusReport("Polishing Reactions (6/9)  ", reaction);
                polish(reaction);
            }
        }

        /**
         * Polishes the reaction by applying various checks and modifications to ensure it conforms to
         * the expected standards and conventions. This includes setting SBO terms, checking compartments,
         * and ensuring proper setup of reactants and products.
         */
        @Override
        public void polish(Reaction reaction) {
            var originalReaction = reaction.clone();
            // Process any external resources linked via annotations in the reaction
            new AnnotationPolisher(polishingParameters, registry).polish(reaction.getAnnotation());

            setMetaId(reaction);

            if (sboParameters.addGenericTerms()) {
                if (reaction.isSetListOfReactants()) {
                    new SpeciesReferencesPolisher(SBO.getReactant()).polish(reaction.getListOfReactants());
                }
                if (reaction.isSetListOfProducts()) {
                    new SpeciesReferencesPolisher(SBO.getProduct()).polish(reaction.getListOfProducts());
                }
            }

            setCompartmentFromReactionParticipants(reaction);

            removeCopySuffix(reaction);

            setSBOTerm(reaction);
        }

        private void setMetaId(Reaction reaction) {
            if (!reaction.isSetMetaId() && (reaction.getCVTermCount() > 0)) {
                reaction.setMetaId(reaction.getId());
            }
        }

        private void setCompartmentFromReactionParticipants(Reaction reaction) {
            if (reaction.getCompartment() == null || reaction.getCompartment().isEmpty()) {
                var reactantsCompartment = getCommonCompartmentCode(reaction.getListOfReactants());
                var productsCompartment = getCommonCompartmentCode(reaction.getListOfProducts());
                if (reactantsCompartment.isEmpty() && productsCompartment.isPresent()) {
                    reaction.setCompartment(productsCompartment.get());
                }
                else if (reactantsCompartment.isPresent() && productsCompartment.isEmpty()) {
                    reaction.setCompartment(reactantsCompartment.get());
                }
                else if (reactantsCompartment.isPresent()
                        && reactantsCompartment.get().equals(productsCompartment.get())) {
                    reaction.setCompartment(reactantsCompartment.get());
                }
            }
        }

        private Optional<String> getCommonCompartmentCode(ListOf<SpeciesReference> speciesReferences) {
            // Attempt to identify a common compartment for all species references
            if (null != speciesReferences) {
                var modelSpecies = speciesReferences.stream()
                        .map(SpeciesReference::getSpeciesInstance)
                        .filter(Objects::nonNull)
                        .map(Species::getCompartmentInstance)
                        .filter(Objects::nonNull        )
                        .map(Compartment::getId)
                        .collect(Collectors.toSet());
                return modelSpecies.size() == 1 ? modelSpecies.stream().findFirst() : Optional.empty();
            }
            return Optional.empty();
        }


        private void removeCopySuffix(Reaction reaction) {
            String rName = reaction.getName();
            if (rName.matches(".*_copy\\d*")) {
                rName = rName.substring(0, rName.lastIndexOf('_'));
                reaction.setName(rName);
            }
        }


        private void setSBOTerm(Reaction reaction) {
            setSBOTermFromPattern(reaction, BiGGId.createReactionId(reaction.getId()));

            if (!reaction.isSetSBOTerm()) {
                if (reaction.getReactantCount() != 0 && reaction.getProductCount() == 0 && !reaction.isReversible()) {
                    reaction.setSBOTerm(632);
                }
                else if (reaction.getReactantCount() == 0 && reaction.getProductCount() != 0 && !reaction.isReversible()) {
                    reaction.setSBOTerm(628);
                }
            }
        }

        /**
         * Sets the Systems Biology Ontology (SBO) term for a reaction based on the abbreviation of its BiGG ID.
         * The method matches the abbreviation against predefined patterns to determine the appropriate SBO term.
         *
         * @param id The BiGGId object containing the abbreviation to be checked.
         */
        private void setSBOTermFromPattern(Reaction reaction, BiGGId id) {
            String abbrev = id.getAbbreviation();
            if (ReactionNamePatterns.BIOMASS_CASE_INSENSITIVE.getPattern().matcher(abbrev).matches()) {
                reaction.setSBOTerm(629); // Set SBO term for biomass production
            } else if (ReactionNamePatterns.DEMAND_REACTION.getPattern().matcher(abbrev).matches()) {
                reaction.setSBOTerm(628); // Set SBO term for demand reaction
            } else if (ReactionNamePatterns.EXCHANGE_REACTION.getPattern().matcher(abbrev).matches()) {
                reaction.setSBOTerm(627); // Set SBO term for exchange reaction
            } else if (ReactionNamePatterns.ATP_MAINTENANCE.getPattern().matcher(abbrev).matches()) {
                reaction.setSBOTerm(630); // Set SBO term for ATP maintenance
            } else if (ReactionNamePatterns.SINK_REACTION.getPattern().matcher(abbrev).matches()) {
                reaction.setSBOTerm(632); // Set SBO term for sink reaction
            }
        }

    }
