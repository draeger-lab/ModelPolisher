package edu.ucsd.sbrg.polishing;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.logging.BundleNames;
import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

import static java.text.MessageFormat.format;


public class SpeciesPolisher extends AbstractPolisher implements IPolishSBases<Species> {
  private static final Logger logger = LoggerFactory.getLogger(SpeciesPolisher.class);
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.POLISHING_MESSAGES);

  public SpeciesPolisher(PolishingParameters parameters, Registry registry) {
      super(parameters, registry);
  }

  public SpeciesPolisher(PolishingParameters parameters, Registry registry, List<ProgressObserver> observers) {
      super(parameters, registry, observers);
  }


  @Override
  public void polish(List<Species> species) {
    logger.debug("Polish Species");
    for (Species s : species) {
      statusReport("Polishing Species (4/9)  ", s); // Update progress display for each species
      polish(s);
    }
  }


  public void polish(Species species) {
    new AnnotationPolisher(polishingParameters, registry).polish(species.getAnnotation());

    if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
      species.setMetaId(species.getId());
    }

    setBoundaryConditions(species);

    var biggId = BiGGId.createMetaboliteId(species.getId());

    setCompartmentFromBiggId(species, biggId);

    ensureCompartmentCodeFromBiggIdReferencesCompartment(species, biggId);
  }

  private void setBoundaryConditions(Species species) {
    if (species.getId().endsWith("_boundary") && !species.isBoundaryCondition()) {
      logger.debug(format(MESSAGES.getString("BOUNDARY_FLAG_MISSING"), species.getId()));
      species.setBoundaryCondition(true);
    }
  }

  private void setCompartmentFromBiggId(Species species, BiGGId biggId) {
    if (biggId.isSetCompartmentCode() && !biggId.getCompartmentCode().equals(species.getCompartment())) {

      logger.debug(format(MESSAGES.getString("CHANGE_COMPART_REFERENCE"),
              species.getId(),
              species.getCompartment(),
              biggId.getCompartmentCode()));

      species.setCompartment(biggId.getCompartmentCode());
    }
  }

  private void ensureCompartmentCodeFromBiggIdReferencesCompartment(Species species, BiGGId biggId) {
    Model model = species.getModel();
    String cId = biggId.getCompartmentCode();

    SBase candidate = model.findUniqueNamedSBase(cId);

    if(candidate != null && !(candidate instanceof Compartment)) {
      // TODO: this is not graceful
      candidate.setId(candidate.getId() + "_non_compartment");
    }

    if (candidate == null && cId != null && !cId.isEmpty()) {
      logger.debug(format(MESSAGES.getString("CREATE_MISSING_COMP"),
              cId, species.getId(), species.getElementName()));

      var compartment = model.createCompartment(cId);
      compartment.setConstant(true); // required attribute

      new CompartmentPolisher(polishingParameters, registry, getObservers()).polish(compartment);
    }
  }
}
