package edu.ucsd.sbrg.polishing;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * This class is responsible for polishing {@link Species} objects in an SBML model to ensure they conform to
 * specific standards and completeness. It handles the annotation processing, ID validation, boundary condition settings,
 * and default attribute assignments for species within the model.
 */
public class SpeciesPolisher extends AbstractPolisher<Species> {

  private static final Logger logger = Logger.getLogger(SpeciesPolisher.class.getName());

  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

  private final List<Species> speciesToRemove = new ArrayList<>();

  public SpeciesPolisher(PolishingParameters parameters, Registry registry) {
      super(parameters, registry);
  }

  public SpeciesPolisher(PolishingParameters parameters, Registry registry, List<ProgressObserver> observers) {
      super(parameters, registry, observers);
  }


  /**
   * Polishes the list of species in the given SBML model. This method iterates through each species,
   * applies polishing operations, and collects species that need to be removed based on the polishing results.
   * Removal is based on criteria defined in the SpeciesPolishing class.
   */
  @Override
  public void polish(List<Species> species) {
    for (Species s : species) {
      statusReport("Polishing Species (4/9)  ", s); // Update progress display for each species
      // Polish each species and collect those that need to be removed
      polish(s);
    }

    species.removeAll(speciesToRemove);
  }

  /**
   * Polishes the properties of a {@link Species} to ensure compliance with standards and completeness.
   * This method processes annotations, checks for missing IDs, sets boundary conditions, and ensures
   * that mandatory attributes are set to default values.
   */
  public void polish(Species species) {
    // Process any external resources linked via annotations in the species
    new AnnotationPolisher(polishingParameters, registry).polish(species.getAnnotation());
    String id = species.getId();
    
    // Check if the species ID is missing and log an error if so
    if (id.isEmpty()) {
      if (species.isSetName()) {
        logger.severe(format(MESSAGES.getString("SPECIES_MISSING_ID"), species.getName()));
      } else {
        logger.severe(MESSAGES.getString("SPECIES_MISSING_ID_NAME"));
      }
      speciesToRemove.add(species);
      return;
    }

    // Warn if the species ID indicates a boundary species but the boundary condition is not set
    if (species.getId().endsWith("_boundary")) {
      logger.warning(format(MESSAGES.getString("SPECIES_ID_INVALID"), id));
      if (!species.isSetBoundaryCondition() || !species.isBoundaryCondition()) {
        logger.warning(format(MESSAGES.getString("BOUNDARY_FLAG_MISSING"), id));
        species.setBoundaryCondition(true);
      }
    } else if (!species.isSetBoundaryCondition()) {
      species.setBoundaryCondition(false);
    }

    // Set default values for mandatory attributes if they are not already set
    if (!species.isSetHasOnlySubstanceUnits()) {
      species.setHasOnlySubstanceUnits(true);
    }
    if (!species.isSetConstant()) {
      species.setConstant(false);
    }
    if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
      species.setMetaId(species.getId());
    }

    // Check and potentially update the compartment reference based on BiGG ID
    BiGGId.createMetaboliteId(id).ifPresent(biggId -> {
      if (biggId.isSetCompartmentCode() && species.isSetCompartment()
        && !biggId.getCompartmentCode().equals(species.getCompartment())) {
        logger.warning(format(MESSAGES.getString("CHANGE_COMPART_REFERENCE"), species.getId(), species.getCompartment(),
          biggId.getCompartmentCode()));
        species.setCompartment(biggId.getCompartmentCode());
      }
    });

    // Check the compartment of the species
    checkCompartment(species);
  }

  /**
   * Checks and sets the compartment for a given species. If the species does not have a compartment set,
   * it attempts to set it using the BiGG ID compartment code. If the compartment is still not set or found,
   * it logs a warning and creates a new compartment.
   *
   * @param species The species whose compartment needs to be checked or set.
   */
  public void checkCompartment(Species species) {
    // Check if the compartment is already set for the species
    if (!species.isSetCompartment()) {
      // Attempt to get the BiGG ID for the species
      Optional<BiGGId> biggId = BiGGId.createMetaboliteId(species.getId());
      boolean setCompartment = false;
      // If BiGG ID is present, check for compartment code and set it
      if (biggId.isPresent()) {
        if (biggId.get().isSetCompartmentCode()) {
          species.setCompartment(biggId.get().getCompartmentCode());
          setCompartment = true;
        }
      }
      // If compartment is not set, exit the method
      if (!setCompartment) {
        return;
      }
    }
    // Get the compartment ID from the species
    String cId = species.getCompartment();
    // Get the model associated with the species
    Model model = species.getModel();

    // If model is not available, exit the method
    if (model == null) {
      return;
    }
    // Find the compartment in the model using the compartment ID
    SBase candidate = model.findUniqueNamedSBase(cId);
    // If the found SBase is a Compartment, polish it
    if (candidate instanceof Compartment) {
      var compartmentPolisher = new CompartmentPolisher(polishingParameters, registry, getObservers());
      compartmentPolisher.polish((Compartment) candidate);
    } else if (candidate == null) {
      // If no compartment is found, log a warning and create a new compartment
      logger.warning(format(MESSAGES.getString("CREATE_MISSING_COMP"), cId, species.getId(), species.getElementName()));
      var compartmentPolisher = new CompartmentPolisher(polishingParameters, registry, getObservers());
      compartmentPolisher.polish(model.createCompartment(cId));
    }
  }
}
