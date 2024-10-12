package de.uni_halle.informatik.biodata.mp.polishing;

import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.zbit.util.ResourceManager;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import de.uni_halle.informatik.biodata.mp.logging.BundleNames;
import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import org.sbml.jsbml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

import static java.text.MessageFormat.format;


public class SpeciesPolisher extends AbstractPolisher implements IPolishSBases<Species> {
  private static final Logger logger = LoggerFactory.getLogger(SpeciesPolisher.class);
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.POLISHING_MESSAGES);

  private final SBOParameters sboParameters;

  public SpeciesPolisher(PolishingParameters parameters, SBOParameters sboParameters, Registry registry, List<ProgressObserver> observers) {
      super(parameters, registry);
    this.sboParameters = sboParameters;
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

    setSBOTerm(species);

    setBoundaryConditions(species);

    var biggId = BiGGId.createMetaboliteId(species.getId());

    setCompartmentFromBiggId(species, biggId);

    ensureCompartmentCodeFromBiggIdReferencesCompartment(species, biggId);
  }

  private void setSBOTerm(Species species) {
    if (!species.isSetSBOTerm() && sboParameters.addGenericTerms()) {
      species.setSBOTerm(SBO.getSimpleMolecule());
    }
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
