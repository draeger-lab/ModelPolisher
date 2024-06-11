package edu.ucsd.sbrg.bigg.polishing;

import de.zbit.kegg.AtomBalanceCheck;
import de.zbit.kegg.AtomBalanceCheck.AtomCheckResult;
import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.bigg.Parameters;
import edu.ucsd.sbrg.miriam.Registry;
import edu.ucsd.sbrg.util.GPRParser;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.util.ResourceManager;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.jsbml.xml.XMLNode;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
 * 
 * The class operates on an SBML {@link Reaction} object and modifies it to conform to standards and conventions
 * used in systems biology models, particularly those related to flux balance constraints.
 */
public class ReactionPolishing {

  private final static transient Logger logger = Logger.getLogger(ReactionPolishing.class.getName());

  private static final transient ResourceBundle MESSAGES =
    de.zbit.util.ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  private final Reaction reaction;

  /**
   * Defines an enumeration for regex patterns that are used to categorize reactions based on their ID strings.
   * Each enum constant represents a specific type of reaction and is associated with a regex pattern that matches
   * reaction IDs corresponding to that type.
   */
  public enum Patterns {

    /**
     * Pattern for ATP maintenance reactions, which are typically denoted by IDs containing 'ATPM' in any case.
     */
    ATP_MAINTENANCE(".*[Aa][Tt][Pp][Mm]"),

    /**
     * Case-insensitive pattern for biomass reactions, matching IDs that include the word 'biomass' in any case.
     */
    BIOMASS_CASE_INSENSITIVE(".*[Bb][Ii][Oo][Mm][Aa][Ss][Ss].*"),

    /**
     * Case-sensitive pattern for biomass reactions, matching IDs that specifically contain 'BIOMASS'.
     */
    BIOMASS_CASE_SENSITIVE(".*BIOMASS.*"),

    /**
     * Pattern for default flux bound reactions, matching IDs that typically start with a prefix followed by 'default_'.
     */
    DEFAULT_FLUX_BOUND("(.*_)?[Dd][Ee][Ff][Aa][Uu][Ll][Tt]_.*"),

    /**
     * Pattern for demand reactions, identified by IDs starting with 'DM_'.
     */
    DEMAND_REACTION("(.*_)?[Dd][Mm]_.*"),

    /**
     * Pattern for exchange reactions, identified by IDs starting with 'EX_'.
     */
    EXCHANGE_REACTION("(.*_)?[Ee][Xx]_.*"),

    /**
     * Pattern for sink reactions, which are reactions that remove metabolites from the system, identified by IDs starting with 'SK_' or 'SINK_'.
     */
    SINK_REACTION("(.*_)?[Ss]([Ii][Nn])?[Kk]_.*");

    /**
     * The compiled regex pattern used for matching reaction IDs.
     */
    private final Pattern pattern;

    /**
     * Constructs a new enum constant with the specified regex pattern.
     *
     * @param regex The regex pattern to compile.
     */
    Patterns(String regex) {
      pattern = Pattern.compile(regex);
    }

    /**
     * Retrieves the compiled Pattern object for this enum constant.
     *
     * @return The compiled Pattern object.
     */
    public Pattern getPattern() {
      return pattern;
    }
  }

  /**
   * Constructs a new {@code ReactionPolishing} instance for the specified reaction.
   * 
   * @param reaction The reaction to be polished.
   */
  public ReactionPolishing(Reaction reaction) {
    this.reaction = reaction;
  }

  /**
   * Polishes the reaction by applying various checks and modifications to ensure it conforms to
   * the expected standards and conventions. This includes setting SBO terms, checking compartments,
   * and ensuring proper setup of reactants and products.
   * 
   * @return {@code true} if the reaction qualifies for strict FBC after polishing, {@code false} otherwise.
   */
  @SuppressWarnings("deprecated")
  public boolean polish() {
    // Process any external resources linked via annotations in the reaction
    Registry.processResources(reaction.getAnnotation());
    
    // Retrieve and check the reaction ID
    String id = reaction.getId();
    if (id.isEmpty()) {
      // Log severe error and remove reaction if ID is missing
      if (reaction.isSetName()) {
        logger.severe(format(MESSAGES.getString("REACTION_MISSING_ID"), reaction.getName()));
      } else {
        logger.severe(MESSAGES.getString("REACTION_MISSING_ID_NAME"));
      }
      reaction.getModel().removeReaction(reaction);
      return false;
    }
    
    // Set the SBO term based on the reaction ID pattern
    BiGGId.createReactionId(id).ifPresent(this::setSBOTermFromPattern);
    
    // Check and set the compartment of the reaction based on its reactants and products
    String compartmentId = reaction.isSetCompartment() ? reaction.getCompartment() : null;
    boolean conflict = false;
    if (reaction.isSetListOfReactants()) {
      Optional<String> cIdFromReactants = polish(reaction.getListOfReactants(), SBO.getReactant());
      conflict = cIdFromReactants.isEmpty();
      if (!conflict && (compartmentId == null || compartmentId.equals(cIdFromReactants.get()))) {
        reaction.setCompartment(cIdFromReactants.get());
      }
    }
    if (reaction.isSetListOfProducts()) {
      Optional<String> cIdFromProducts = polish(reaction.getListOfProducts(), SBO.getProduct());
      conflict |= cIdFromProducts.isEmpty();
      if (!conflict && (compartmentId == null || compartmentId.equals(cIdFromProducts.get()))) {
        reaction.setCompartment(cIdFromProducts.get());
      } else {
        reaction.unsetCompartment();
      }
    }
    
    // Set meta ID if not set and CV terms are present
    if (!reaction.isSetMetaId() && (reaction.getCVTermCount() > 0)) {
      reaction.setMetaId(reaction.getId());
    }
    
    // Remove '_copy' suffix from reaction name if present
    String rName = reaction.getName();
    if (rName.matches(".*_copy\\d*")) {
      rName = rName.substring(0, rName.lastIndexOf('_'));
      reaction.setName(rName);
    }
    
    // Ensure reaction properties are set according to SBML Level and Version
    if ((!reaction.isSetLevelAndVersion()
            || reaction.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) <= 0)
            && !reaction.isSetFast()) {
      reaction.setFast(false);
    }
    if (!reaction.isSetReversible()) {
      reaction.setReversible(false);
    }
    
    // Check for reactions without reactants or products and log severe error if found
    if ((reaction.getReactantCount() == 0) && (reaction.getProductCount() == 0)) {
      ResourceBundle bundle = ResourceManager.getBundle("org.sbml.jsbml.resources.cfg.Messages");
      logger.severe(format(bundle.getString("SBMLCoreParser.reactionWithoutParticipantsError"), reaction.getId()));
    } else {
      checkBalance();
    }
    
    // Initialize strict mode flag
    boolean strict = false;
    if (reaction.getModel() != null) {
      // Convert gene associations to FBCv2 format and set flux objectives from local parameters
      GPRParser.convertAssociationsToFBCV2(reaction, Parameters.get().omitGenericTerms());
      fluxObjectiveFromLocalParameter();
      associationFromNotes();
      strict = checkBounds();
    }
    
    // Check validity of reactants and products and update strict mode flag
    strict = checkReactantsProducts(strict);
    return strict;
  }

  /**
   * Sets the Systems Biology Ontology (SBO) term for a reaction based on the abbreviation of its BiGG ID.
   * The method matches the abbreviation against predefined patterns to determine the appropriate SBO term.
   * 
   * @param id The BiGGId object containing the abbreviation to be checked.
   */
  private void setSBOTermFromPattern(BiGGId id) {
    String abbrev = id.getAbbreviation();
    if (Patterns.BIOMASS_CASE_INSENSITIVE.getPattern().matcher(abbrev).matches()) {
      reaction.setSBOTerm(629); // Set SBO term for biomass production
    } else if (Patterns.DEMAND_REACTION.getPattern().matcher(abbrev).matches()) {
      reaction.setSBOTerm(628); // Set SBO term for demand reaction
    } else if (Patterns.EXCHANGE_REACTION.getPattern().matcher(abbrev).matches()) {
      reaction.setSBOTerm(627); // Set SBO term for exchange reaction
    } else if (Patterns.ATP_MAINTENANCE.getPattern().matcher(abbrev).matches()) {
      reaction.setSBOTerm(630); // Set SBO term for ATP maintenance
    } else if (Patterns.SINK_REACTION.getPattern().matcher(abbrev).matches()) {
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
  private Optional<String> polish(ListOf<SpeciesReference> speciesReferences, int defaultSBOterm) {
    // Assign default SBO terms and constant values to species references
    for (SpeciesReference sr : speciesReferences) {
      if (!sr.isSetSBOTerm() && !Parameters.get().omitGenericTerms()) {
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
  private void checkBalance() {
    // Check if the reaction SBO term is not set
    if (!reaction.isSetSBOTerm()) {
      // Check if there are no reactants
      if (reaction.getReactantCount() == 0) {
        // Handle reversible reactions differently
        if (reaction.isReversible()) {
          // Placeholder for handling sink reactions
          // TODO: Implement sink reaction handling
        } else if (reaction.getSBOTerm() != 628) {
          // Log and set SBO term for demand reaction if not already set
          // logger.info(format(mpMessageBundle.getString("REACTION_DM_NOT_IN_ID"), reaction.getId()));
          reaction.setSBOTerm(628); // Set as demand reaction
        }
      } else if (reaction.getProductCount() == 0) {
        // Handle reversible reactions differently
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
    // Check mass balance if enabled in parameters and reaction is not a special type
    if (Parameters.get().checkMassBalance()
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

  /**
   * This method sets the flux objective and its coefficient for a reaction based on the kinetic law parameters.
   * If the reaction does not already have a flux objective, this method will create one and set it to maximize.
   * It then checks if a flux objective already exists for the reaction. If not, it attempts to retrieve the
   * "OBJECTIVE_COEFFICIENT" from the reaction's kinetic law and uses it to create and set a new flux objective
   * with the retrieved coefficient value.
   */
  private void fluxObjectiveFromLocalParameter() {
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
  private void associationFromNotes() {
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
                  GPRParser.parseGPR(reaction, association, Parameters.get().omitGenericTerms());
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
   *
   * @return true if both lower and upper flux bounds exist and are strictly defined, false otherwise.
   */
  private boolean checkBounds() {
    FBCReactionPlugin rPlug = (FBCReactionPlugin) reaction.getPlugin(FBCConstants.shortLabel);
    Parameter lb = rPlug.getLowerFluxBoundInstance();
    Parameter ub = rPlug.getUpperFluxBoundInstance();
    boolean lbExists = polishFluxBound(lb);
    // set bounds from KineticLaw, if they are not set in FBC, create global Parameter,
    // as required by specification
    if (!lbExists) {
      LocalParameter bound = getBoundFromLocal(reaction, "LOWER_BOUND");
      if (bound != null) {
        lb = new Parameter(bound);
        Parameter existingParameter = getParameterVariant(reaction, lb, bound.getValue());
        if (existingParameter != null) {
          rPlug.setLowerFluxBound(existingParameter);
        } else {
          reaction.getModel().addParameter(lb);
          rPlug.setLowerFluxBound(lb);
        }
        lbExists = polishFluxBound(rPlug.getLowerFluxBoundInstance());
      }
    }
    boolean ubExists = polishFluxBound(ub);
    if (!ubExists) {
      LocalParameter bound = getBoundFromLocal(reaction, "UPPER_BOUND");
      if (bound != null) {
        ub = new Parameter(bound);
        Parameter existingParameter = getParameterVariant(reaction, ub, bound.getValue());
        if (existingParameter != null) {
          rPlug.setUpperFluxBound(existingParameter);
        } else {
          reaction.getModel().addParameter(ub);
          rPlug.setUpperFluxBound(ub);
        }
        ubExists = polishFluxBound(rPlug.getUpperFluxBoundInstance());
      }
    }
    boolean strict = lbExists && ubExists;
    if (strict) {
      strict = checkBound(lb) && lb.getValue() < Double.POSITIVE_INFINITY && checkBound(ub)
        && ub.getValue() > Double.NEGATIVE_INFINITY && lb.getValue() <= ub.getValue();
      if (!strict) {
        logger.warning(format(MESSAGES.getString("FLUX_BOUND_ERROR"), reaction.getId()));
      }
    } else {
      logger.warning(format(MESSAGES.getString("FLUX_BOUNDS_MISSING"), reaction.getId()));
    }
    return strict;
  }


  /**
   * Polishes the SBO term of a flux bound parameter based on its ID.
   * If the parameter's ID matches the default flux bound pattern, it sets the SBO term to 626.
   * Otherwise, it sets the SBO term to 625.
   *
   * @param bound The parameter representing a flux bound.
   * @return {@code true} if the parameter is not null and was successfully updated; {@code false} if the parameter is null.
   */
  public boolean polishFluxBound(Parameter bound) {
    if (bound == null) {
      return false;
    }
    if (Patterns.DEFAULT_FLUX_BOUND.getPattern().matcher(bound.getId()).matches()) {
      bound.setSBOTerm(626); // default flux bound
    } else {
      bound.setSBOTerm(625); // flux bound
    }
    return true;
  }

  /**
   * Retrieves a local parameter from a reaction's kinetic law based on the specified parameter name.
   * This method specifically looks for parameters that define either the lower or upper flux bounds.
   *
   * @param r The reaction from which the kinetic law and the parameter are to be retrieved.
   * @param parameterName The name of the parameter to retrieve, expected to be either "LOWER_BOUND" or "UPPER_BOUND".
   * @return The local parameter if found, or {@code null} if the kinetic law is not defined or the parameter does not exist.
   */
  private LocalParameter getBoundFromLocal(Reaction r, String parameterName) {
    KineticLaw kl = r.getKineticLaw();
    if (kl != null) {
      return kl.getLocalParameter(parameterName);
    }
    return null;
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
   * @return The {@link Parameter} with its ID modified based on the bound value.
   */
  private Parameter getParameterVariant(Reaction r, Parameter bound, double boundValue) {
    if (boundValue == -1000d) {
      bound.setId("DEFAULT_LOWER_BOUND");
    } else if (boundValue == 0d) {
      bound.setId("DEFAULT_BOUND");
    } else if (boundValue == 1000d) {
      bound.setId("DEFAULT_UPPER_BOUND");
    } else {
      bound.setId(r.getId() + "_" + bound.getId());
    }
    return r.getModel().getParameter(bound.getId());
  }

  /**
   * Evaluates whether a {@link Parameter} instance meets the criteria to be considered a valid strict flux bound.
   * A strict flux bound parameter must:
   * <ul>
   * <li>Not be null</li>
   * <li>Be constant</li>
   * <li>Have a defined value that is not {@link Double#NaN}</li>
   * </ul>
   * This method is used to ensure parameters can be reliably used in strict FBC (Flux Balance Constraints) models.
   *
   * @param bound The {@link Parameter} to check.
   * @return {@code true} if the parameter qualifies as a strict flux bound, {@code false} otherwise.
   */
  public boolean checkBound(Parameter bound) {
    return (bound != null) && bound.isConstant()
            && bound.isSetValue()
            && !Double.isNaN(bound.getValue());
  }


  /**
   * Checks the validity of reactants and products in a reaction based on specified criteria.
   * This method evaluates whether all reactants and products meet the criteria defined in {@link #checkSpeciesReferences(ListOf)}.
   * If any reactant or product does not meet the criteria, a warning is logged.
   *
   * @param strict A boolean flag indicating whether the check should be strictly enforced.
   * @return {@code true} if all reactants and products meet the criteria when strict is {@code true}, {@code false} otherwise.
   */
  private boolean checkReactantsProducts(boolean strict) {
    if (strict && reaction.isSetListOfReactants()) {
      strict = checkSpeciesReferences(reaction.getListOfReactants());
      if (!strict) {
        logger.warning(format(MESSAGES.getString("ILLEGAL_STOICH_REACT"), reaction.getId()));
      }
    }
    if (strict && reaction.isSetListOfProducts()) {
      strict = checkSpeciesReferences(reaction.getListOfProducts());
      if (!strict) {
        logger.warning(format(MESSAGES.getString("ILLEGAL_STOICH_PROD"), reaction.getId()));
      }
    }
    return strict;
  }
  
  /**
   * Checks if all species references in a list meet certain criteria.
   * Each species reference must:
   * - Be constant.
   * - Have a set stoichiometry.
   * - Have a value that is not NaN (Not a Number).
   * - Have a value that is finite.
   *
   * @param listOfSpeciesReference The list of {@link SpeciesReference} objects to check.
   * @return {@code true} if all species references in the list meet the criteria, {@code false} otherwise.
   */
  public boolean checkSpeciesReferences(ListOf<SpeciesReference> listOfSpeciesReference) {
    boolean strict = true;
    for (SpeciesReference sr : listOfSpeciesReference) {
      strict &=
        sr.isConstant()
                && sr.isSetStoichiometry()
                && !Double.isNaN(sr.getValue())
                && Double.isFinite(sr.getValue());
    }
    return strict;
  }
}
