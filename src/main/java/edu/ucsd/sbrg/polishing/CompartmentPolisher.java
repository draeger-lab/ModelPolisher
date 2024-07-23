package edu.ucsd.sbrg.polishing;

import static java.text.MessageFormat.format;

import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.List;

import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Unit;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.db.bigg.BiGGId;

/**
 * This class is responsible for polishing the properties of a compartment in an SBML model to ensure
 * compliance with standards and completeness. It handles the annotation processing, ID and name setting,
 * and ensures that necessary attributes like units and spatial dimensions are appropriately set.
 */
public class CompartmentPolisher extends AbstractPolisher<Compartment> {

  private final static Logger logger = Logger.getLogger(CompartmentPolisher.class.getName());
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

  public CompartmentPolisher(PolishingParameters polishingParameters, Registry registry, List<ProgressObserver> observers) {
    super(polishingParameters, registry, observers);
  }


  /**
   * Polishes all compartments in the given SBML model. This method iterates through each compartment
   * in the model, updates the progress display, and applies polishing operations defined in the
   * CompartmentPolishing class.
   */
  @Override
  public void polish(List<Compartment> compartments) {
    for (Compartment compartment : compartments) {
      statusReport("Polishing Compartments (3/9)  ", compartment);
      polish(compartment);
    }
  }


  /**
   * Polishes the properties of a compartment to ensure compliance with standards and completeness.
   * This method processes annotations, sets default values for missing identifiers, names, and meta identifiers,
   * and ensures that the compartment has appropriate units and other necessary attributes set.
   */
  @Override
  public void polish(Compartment compartment) {
    // Process any external resources linked via annotations in the compartment
    new AnnotationPolisher(polishingParameters, registry).polish(compartment.getAnnotation());

    // Set a default ID if not already set, otherwise clean up the ID according to BiGG specifications
    if (!compartment.isSetId()) {
      compartment.setId("d"); // default ID if none is set
    } else {
      // Attempt to remove the 'C_' prefix from the compartment ID, log a warning if the format is incorrect
      BiGGId.extractCompartmentCode(compartment.getId()).ifPresentOrElse(compartment::setId,
        () -> logger.warning(format(MESSAGES.getString("COMPARTMENT_CODE_WRONG_FORMAT"), compartment.getId())));
    }

    // Set the SBOTerm to indicate an implicit compartment
    compartment.setSBOTerm(410);

    // Set a default name if not already set
    if (!compartment.isSetName()) {
      compartment.setName("default");
    }

    // Set the metaId to the compartment's ID if it has CV terms but no metaId set
    if (!compartment.isSetMetaId() && (compartment.getCVTermCount() > 0)) {
      compartment.setMetaId(compartment.getId());
    }

    // Ensure the compartment's 'constant' property is set to true if not already specified
    if (!compartment.isSetConstant()) {
      compartment.setConstant(true);
    }

    // TODO: Implement logic to set spatial dimensions based on BiGG ID, considering special cases like surfaces
    if (!compartment.isSetSpatialDimensions()) {
      // Placeholder for future implementation
      // compartment.setSpatialDimensions(3d);
    }

    // Set the units of the compartment to dimensionless if no specific units are set in the model
    if (!compartment.isSetUnits()) {
      Model model = compartment.getModel();
      if ((model == null) || !(model.isSetLengthUnits() || model.isSetAreaUnits() || model.isSetVolumeUnits())) {
        compartment.setUnits(Unit.Kind.DIMENSIONLESS);
      }
    }
  }

}
