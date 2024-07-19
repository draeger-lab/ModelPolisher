package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrgURI;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.*;

import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.db.MemorizedQuery;

import java.util.List;

/**
 * This class is responsible for annotating a specific compartment within an SBML model using data from the BiGG database.
 * It allows for the addition of both BiGG and SBO annotations to a compartment, and can also set the compartment's name
 * based on information retrieved from the BiGG database.
 */
public class CompartmentsAnnotator extends AbstractAnnotator<Compartment>{


  public CompartmentsAnnotator(Parameters parameters, Registry registry) {
    super(parameters, registry);
  }

  public CompartmentsAnnotator(Parameters parameters, Registry registry, List<ProgressObserver> observers) {
    super(parameters, registry, observers);
  }

  public void annotate(List<Compartment> compartments) {
      for (Compartment compartment : compartments) {
        statusReport("Annotating Compartments (2/5)  ", compartment);
        annotate(compartment);
      }
  }

  /**
   * Annotates the compartment with BiGG and SBO terms. If the compartment's name is not set or is set to "default",
   * it updates the name based on the BiGG database. This method only processes compartments that are recognized
   * within the BiGG Knowledgebase.
   */
  public void annotate(Compartment compartment) {
    BiGGId biggId = new BiGGId(compartment.getId());
    if (MemorizedQuery.isCompartment(biggId.getAbbreviation())) {
      compartment.addCVTerm(
              new CVTerm(CVTerm.Qualifier.BQB_IS,
              new IdentifiersOrgURI("bigg.compartment", biggId).getURI()));
      compartment.setSBOTerm(SBO.getCompartment()); // physical compartment
      if (!compartment.isSetName() || compartment.getName().equals("default")) {
        BiGGDB.getCompartmentName(biggId).ifPresent(compartment::setName);
      }
    }
  }
}
