package edu.ucsd.sbrg.polishing;

import de.zbit.util.ResourceManager;
import de.zbit.util.progressbar.AbstractProgressBar;
import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.util.ProgressObserver;
import edu.ucsd.sbrg.util.ProgressUpdate;
import edu.ucsd.sbrg.miriam.Registry;
import edu.ucsd.sbrg.util.SBMLFix;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.Objective;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * This class provides methods to polish an SBML model to conform to specific standards and conventions.
 * It handles tasks such as processing annotations, setting meta identifiers, and polishing various model components
 * like initial assignments, objectives, gene products, and parameters. The class operates on an SBML {@link Model}
 * object and modifies it to enhance its structure and metadata based on the provided configurations.
 * 
 * Progress of the polishing process can be visually tracked using an {@link AbstractProgressBar} which is updated
 * throughout the various stages of the polishing process.
 */
public class MiscPolishing {

  private static final transient Logger logger = Logger.getLogger(MiscPolishing.class.getName());

  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

  private List<ProgressObserver> observers = new ArrayList<>();

  private final Model model;

  private boolean strict;
  private final Parameters parameters;

  public MiscPolishing(Model model, boolean strict, List<ProgressObserver> observers, Parameters parameters) {
    this.model = model;
    this.strict = strict;
    this.observers = observers;
    this.parameters = parameters;
  }

  public MiscPolishing(Model model, boolean strict, Parameters parameters) {
    this.model = model;
    this.strict = strict;
    this.parameters = parameters;
  }


  /**
   * Polishes the SBML model by processing annotations, setting meta identifiers, and polishing various components.
   * This method processes the model's annotations, sets the model's meta identifier if not already set and CV terms are present.
   * It conditionally polishes lists of initial assignments, objectives, gene products, and parameters based on the model's configuration.
   */
  public void polish() {
    // Process the annotations in the model
    Registry.processResources(model.getAnnotation());
    // Set the metaId of the model if it is not set and there are CV terms
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }
    // Polish the list of initial assignments if strict mode is enabled and the list is set
    if (strict && model.isSetListOfInitialAssignments()) {
      polishListOfInitialAssignments();
    }
    // Check if the FBC plugin is set and proceed with polishing specific FBC components
    if (model.isSetPlugin(FBCConstants.shortLabel)) {
      FBCModelPlugin modelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
      // Polish the list of objectives if set
      if (modelPlug.isSetListOfObjectives()) {
        polishListOfObjectives(modelPlug);
      }
      // Polish the list of gene products if set
      if (modelPlug.isSetListOfGeneProducts()) {
        polishListOfGeneProducts(modelPlug);
      }
      // Apply strictness setting to the FBC model plugin
      modelPlug.setStrict(strict);
    }
    // Polish the list of parameters in the model
    polishListOfParameters(model);
  }

  /**
   * Polishes the list of initial assignments in the model.
   * This method iterates through each initial assignment and performs checks on the associated variable.
   * If the variable is a parameter with a specific SBO term, or if it's a species reference, 
   * the strict mode is disabled and appropriate warnings are logged.
   */
  public void polishListOfInitialAssignments() {
    for (InitialAssignment ia : model.getListOfInitialAssignments()) {
      // Update progress display
      updateProgressObservers("Polishing Initial Assignments (6/9)  ", ia);
      // Retrieve the variable associated with the initial assignment
      Variable variable = ia.getVariableInstance();
      if (variable != null) {
        // Check if the variable is a parameter with a specific Systems Biology Ontology (SBO) term
        if (variable instanceof Parameter) {
          if (variable.isSetSBOTerm() && SBO.isChildOf(variable.getSBOTerm(), 625)) {
            // Disable strict mode and log a warning if the SBO term indicates a flux boundary condition
            strict = false;
            logger.warning(format(MESSAGES.getString("FLUX_BOUND_STRICT_CHANGE"), variable.getId()));
          }
        } else if (variable instanceof SpeciesReference) {
          // Disable strict mode if the variable is a species reference
          strict = false;
        }
      }
    }
  }
  /**
   * Polishes the list of objectives in the given FBC model plugin.
   * This method checks for the presence of objectives and processes each one.
   * If no objectives are present, a warning is logged.
   * Each objective is checked for the presence of flux objectives, and if absent, attempts to fix them.
   * Objectives without any flux objectives are removed from the model.
   *
   * @param modelPlug The FBCModelPlugin containing the list of objectives to be polished.
   */
  public void polishListOfObjectives(FBCModelPlugin modelPlug) {
    if (modelPlug.getObjectiveCount() == 0) {
      // Note: the strict attribute does not require the presence of any Objectives in the model.
      logger.warning(format(MESSAGES.getString("OBJ_MISSING"), modelPlug.getParent().getId()));
    } else {
      for (var objective : modelPlug.getListOfObjectives()) {
        updateProgressObservers("Polishing Objectives (7/9)  ", objective); // "Processing objective " + objective.getId());
        if (!objective.isSetListOfFluxObjectives()) {
          Model model = modelPlug.getParent();
          strict &= SBMLFix.fixObjective(model.getId(), model.getListOfReactions(), modelPlug,
            parameters.fluxCoefficients(), parameters.fluxObjectives());
        }
        if (objective.isSetListOfFluxObjectives() || objective.getListOfFluxObjectives().isEmpty()) {
          polishListOfFluxObjectives(objective);
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
  }

  /**
   * Polishes the list of flux objectives within a given objective.
   * This method checks for the presence and validity of flux objectives and logs warnings if:
   * - No flux objectives are present.
   * - There are more than one flux objectives.
   * - Flux objectives have invalid coefficients.
   *
   * @param objective The objective whose flux objectives are to be polished.
   */
  public void polishListOfFluxObjectives(Objective objective) {
    if (objective.getFluxObjectiveCount() == 0) {
      // Note: the strict attribute does not require the presence of any flux objectives.
      logger.warning(format(MESSAGES.getString("OBJ_FLUX_OBJ_MISSING"), objective.getId()));
    } else {
      if (objective.getFluxObjectiveCount() > 1) {
        logger.warning(format(MESSAGES.getString("TOO_MUCH_OBJ_TARGETS"), objective.getId()));
      }
      for (FluxObjective fluxObjective : objective.getListOfFluxObjectives()) {
        if (!fluxObjective.isSetCoefficient() || Double.isNaN(fluxObjective.getCoefficient())
          || !Double.isFinite(fluxObjective.getCoefficient())) {
          logger.warning(format(MESSAGES.getString("FLUX_OBJ_COEFF_INVALID"), fluxObjective.getReaction()));
        }
      }
    }
  }

  /**
   * Polishes the list of gene products in the given FBC model plugin.
   * This method iterates through each gene product, displays the progress,
   * and applies the polishing process to each gene product.
   *
   * @param fbcModelPlug The FBCModelPlugin containing the list of gene products to be polished.
   */
  public void polishListOfGeneProducts(FBCModelPlugin fbcModelPlug) {
    for (GeneProduct geneProduct : fbcModelPlug.getListOfGeneProducts()) {
      updateProgressObservers("Polishing Gene Products (8/9)  ", geneProduct);
      new GeneProductPolishing(geneProduct).polish();
    }
  }

  /**
   * Iterates over all parameters in the model and polishes each one.
   * Displays progress for each parameter polished.
   *
   * @param model The model containing the parameters to be polished.
   */
  public void polishListOfParameters(Model model) {
    for (Parameter parameter : model.getListOfParameters()) {
      updateProgressObservers("Polishing Parameters (9/9)  ", parameter);
      polish(parameter);
    }
  }

  /**
   * Polishes the name of a parameter if it is not already set.
   * This method checks if the parameter has an ID but no name.
   * If the condition is true, it sets the parameter's name to a polished version of its ID.
   * The polishing is done using the {@link PolishingUtils#polishName(String)} method.
   *
   * @param p The parameter to be polished.
   */
  private void polish(Parameter p) {
    if (p.isSetId() && !p.isSetName()) {
      p.setName(PolishingUtils.polishName(p.getId()));
    }
  }

  private void updateProgressObservers(String text, AbstractSBase obj) {
    for (var o : observers) {
      o.update(new ProgressUpdate(text, obj));
    }
  }

}
