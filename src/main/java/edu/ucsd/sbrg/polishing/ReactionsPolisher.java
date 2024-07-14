package edu.ucsd.sbrg.polishing;

import de.zbit.kegg.AtomBalanceCheck;
import de.zbit.kegg.AtomBalanceCheck.AtomCheckResult;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.util.GPRParser;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.jetbrains.annotations.Nullable;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.xml.XMLNode;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

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
public class ReactionsPolisher extends AbstractPolisher<Reaction> {

  private final static Logger logger = Logger.getLogger(ReactionsPolisher.class.getName());
  private static final ResourceBundle MESSAGES = de.zbit.util.ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

  private final GeneProductAssociationsProcessor gpaPolisher;

  public ReactionsPolisher(Parameters parameters) {
      super(parameters);
      this.gpaPolisher = new GeneProductAssociationsProcessor();
  }

  public ReactionsPolisher(Parameters parameters, List<ProgressObserver> observers) {
    super(parameters, observers);
    this.gpaPolisher = new GeneProductAssociationsProcessor();
  }

  /**
   * Polishes all reactions in the given SBML model. This method iterates through each reaction,
   * updates the progress display, and applies polishing operations defined in the ReactionPolishing class.
   * It also aggregates a strictness flag that indicates if all reactions conform to strict FBC (Flux Balance Constraints) standards.
   */
  @Override
  public void polish(List<Reaction> reactions) {
    var iterator = reactions.iterator();
    while (iterator.hasNext()) {
      var reaction = iterator.next();
      updateProgressObservers("Polishing Reactions (5/9)  ", reaction);

      // remove reaction if ID is missing
      String id = reaction.getId();
      if (id.isEmpty()) {
        if (reaction.isSetName()) {
          logger.severe(format(MESSAGES.getString("REACTION_MISSING_ID"), reaction.getName()));
        } else {
          logger.severe(MESSAGES.getString("REACTION_MISSING_ID_NAME"));
        }
        iterator.remove();
      } else {
        polish(reaction);
      }
    }
  }

  /**
   * Polishes the reaction by applying various checks and modifications to ensure it conforms to
   * the expected standards and conventions. This includes setting SBO terms, checking compartments,
   * and ensuring proper setup of reactants and products.
   *
   */
  @SuppressWarnings("deprecated")
  public void polish(Reaction reaction) {
      // Process any external resources linked via annotations in the reaction
      new AnnotationPolisher(parameters).polish(reaction.getAnnotation());
      // Check and set the compartment of the reaction based on its reactants and products
      polishCompartments(reaction);
      // Set meta ID if not set and CV terms are present
      setMetaId(reaction);
      // Remove '_copy' suffix from reaction name if present
      removeCopySuffix(reaction);
      // Ensure reaction properties are set according to SBML Level and Version
      setFastProperty(reaction);
      setReversibleProperty(reaction);
      // check mass balance of the reaction - no-op
      checkBalance(reaction);
      // Convert gene associations to FBCv2 format and set flux objectives from local parameters
      gpaPolisher.convertAssociationsToFBCV2(reaction, parameters.omitGenericTerms());

      fluxObjectiveFromLocalParameter(reaction);

      associationFromNotes(reaction);

      setSBOTerm(reaction);

      polishBounds(reaction);
  }

  private void polishCompartments(Reaction reaction) {
      String compartmentId = reaction.isSetCompartment() ? reaction.getCompartment() : null;
      boolean conflict = false;
      if (reaction.isSetListOfReactants()) {
          Optional<String> cIdFromReactants = polishSpeciesReferences(reaction.getListOfReactants(), SBO.getReactant());
          conflict = cIdFromReactants.isEmpty();
          if (!conflict && (compartmentId == null || compartmentId.equals(cIdFromReactants.get()))) {
              reaction.setCompartment(cIdFromReactants.get());
          }
      }
      if (reaction.isSetListOfProducts()) {
          Optional<String> cIdFromProducts = polishSpeciesReferences(reaction.getListOfProducts(), SBO.getProduct());
          conflict |= cIdFromProducts.isEmpty();
          if (!conflict && (compartmentId == null || compartmentId.equals(cIdFromProducts.get()))) {
              reaction.setCompartment(cIdFromProducts.get());
          } else {
              reaction.unsetCompartment();
          }
      }
  }


  private void setReversibleProperty(Reaction reaction) {
    if (!reaction.isSetReversible()) {
      reaction.setReversible(false);
    }
  }


  private void setFastProperty(Reaction reaction) {
    if ((!reaction.isSetLevelAndVersion()
            || reaction.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) <= 0)
            && !reaction.isSetFast()) {
      reaction.setFast(false);
    }
  }

  private void removeCopySuffix(Reaction reaction) {
    String rName = reaction.getName();
    if (rName.matches(".*_copy\\d*")) {
      rName = rName.substring(0, rName.lastIndexOf('_'));
      reaction.setName(rName);
    }
  }

  private void setMetaId(Reaction reaction) {
    if (!reaction.isSetMetaId() && (reaction.getCVTermCount() > 0)) {
      reaction.setMetaId(reaction.getId());
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

  /**
   * This method polishes a list of {@link SpeciesReference} objects, which represent either reactants or products in a reaction.
   * It sets default SBO terms and constant values for each species reference, and attempts to determine a common compartment
   * for the reaction based on these species references. If all species references are associated with the same compartment,
   * this compartment code is returned. Otherwise, it returns an empty {@link Optional}.
   *
   * @param speciesReferences A {@link ListOf<SpeciesReference>} containing reactants or products of a reaction.
   * @param defaultSBOterm The default Systems Biology Ontology (SBO) term to assign to species references if not already set.
   * @return An {@link Optional<String>} containing the compartment code if it can be unambiguously determined; otherwise, {@link Optional#empty()}.
   */
  private Optional<String> polishSpeciesReferences(ListOf<SpeciesReference> speciesReferences, int defaultSBOterm) {
    // Assign default SBO terms and constant values to species references
    for (SpeciesReference sr : speciesReferences) {
      if (!sr.isSetSBOTerm() && !parameters.omitGenericTerms()) {
        sr.setSBOTerm(defaultSBOterm);
      }
      if (!sr.isSetConstant()) {
        sr.setConstant(false);
      }
    }
    // Attempt to identify a common compartment for all species references
    Model model = speciesReferences.getModel();
    if (null != model) {
      var modelSpecies = speciesReferences.stream()
              .map(SpeciesReference::getSpeciesInstance)
              .map(Optional::ofNullable)
              .map(o -> o.map(Species::getCompartmentInstance))
              .flatMap(Optional::stream)
              .map(Compartment::getId)
              .collect(Collectors.toSet());

      return modelSpecies.size() == 1 ? modelSpecies.stream().findFirst() : Optional.empty();
    }
    return Optional.empty();
  }

  /**
   * Checks the balance of the reaction based on its SBO term and reactant/product counts.
   * It sets the SBO term for demand reactions if not already set and checks the atom balance
   * for reactions not identified as biomass production, demand, exchange, or ATP maintenance.
   */
  private void checkBalance(Reaction reaction) {
    // TODO: logging this information is nonsense, this should be available as output
    // Check mass balance if enabled in parameters and reaction is not a special type
    if (parameters.checkMassBalance()
            && ((reaction.getSBOTerm() < 627) || (630 < reaction.getSBOTerm()))) {
      // Perform atom balance check
      AtomCheckResult<Reaction> defects = AtomBalanceCheck.checkAtomBalance(reaction, 1);
      if ((defects != null) && (defects.hasDefects())) {
        // Log warning if atom defects are found
        logger.warning(format(MESSAGES.getString("ATOMS_MISSING"), reaction.getId(), defects.getDefects().toString()));
      } else if (defects == null) {
        // Log failure to check atom balance
        logger.fine(format(MESSAGES.getString("CHECK_ATOM_BALANCE_FAILED"), reaction.getId()));
      } else {
        // Log successful atom balance check
        logger.fine(format(MESSAGES.getString("ATOMS_OK"), reaction.getId()));
      }
    }
  }

  private void setSBOTerm(Reaction reaction) {
    // Set the SBO term based on the reaction ID pattern
    BiGGId.createReactionId(reaction.getId()).ifPresent(id -> setSBOTermFromPattern(reaction, id));

    if (!reaction.isSetSBOTerm()) {
      if (reaction.getReactantCount() == 0) {
        if (reaction.isReversible()) {
          // Placeholder for handling sink reactions
          // TODO: Implement sink reaction handling
        } else {
          // Log and set SBO term for demand reaction if not already set
          // logger.info(format(mpMessageBundle.getString("REACTION_DM_NOT_IN_ID"), reaction.getId()));
          reaction.setSBOTerm(628); // Set as demand reaction
        }
      } else if (reaction.getProductCount() == 0) {
        if (reaction.isReversible()) {
          // Placeholder for handling source reactions
          // TODO: Implement source reaction handling
        } else {
          // Log and set SBO term for demand reaction if not already set
          // logger.warning(format(mpMessageBundle.getString("REACTION_DM_NOT_IN_ID"), reaction.getId()));
          reaction.setSBOTerm(628); // Set as demand reaction
        }
      }
    }
  }

  /**
   * This method sets the flux objective and its coefficient for a reaction based on the kinetic law parameters.
   * If the reaction does not already have a flux objective, this method will create one and set it to maximize.
   * It then checks if a flux objective already exists for the reaction. If not, it attempts to retrieve the
   * "OBJECTIVE_COEFFICIENT" from the reaction's kinetic law and uses it to create and set a new flux objective
   * with the retrieved coefficient value.
   */
  private void fluxObjectiveFromLocalParameter(Reaction reaction) {
    // Retrieve the FBC model plugin from the reaction's model
    FBCModelPlugin modelPlugin = (FBCModelPlugin) reaction.getModel().getPlugin(FBCConstants.shortLabel);
    // Attempt to get the first objective, or create one if none exist
    Objective obj = modelPlugin.getObjective(0);
    if (obj == null) {
      obj = modelPlugin.createObjective("obj");
      obj.setType(Objective.Type.MAXIMIZE);
      modelPlugin.getListOfObjectives().setActiveObjective(obj.getId());
    }
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

  /**
   * This method extracts gene associations from the notes of a reaction and converts them into
   * the FBCv2 GeneProductAssociation format. It specifically looks for notes tagged with "GENE_ASSOCIATION:"
   * and processes them to set the gene product association for the reaction if it has not been set already.
   */
  private void associationFromNotes(Reaction reaction) {
    // Obtain the FBC plugin for the reaction to handle FBC-specific features.
    FBCReactionPlugin reactionPlugin = (FBCReactionPlugin) reaction.getPlugin(FBCConstants.shortLabel);

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
                  GPRParser.parseGPR(reaction, association, parameters.omitGenericTerms());
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


  private @Nullable Parameter ensureBound(Reaction reaction, String boundType) {
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
   * @param r The reaction from which the kinetic law and the parameter are to be retrieved.
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
   * @param r The {@link Reaction} instance from which the model and parameter are derived.
   * @param bound The {@link Parameter} instance representing either a lower or upper bound.
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
