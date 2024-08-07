package edu.ucsd.sbrg.annotation.bigg;

import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrgURI;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.*;

import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.db.bigg.BiGGDB;

import java.sql.SQLException;
import java.util.List;

/**
 * This class is responsible for annotating a specific compartment within an SBML model using data from the BiGG database.
 * It allows for the addition of both BiGG and SBO annotations to a compartment, and can also set the compartment's name
 * based on information retrieved from the BiGG database.
 */
public class BiGGCompartmentsAnnotator extends AbstractBiGGAnnotator<Compartment> {


  public BiGGCompartmentsAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, Registry registry) {
    super(bigg, parameters, registry);
  }

  public BiGGCompartmentsAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, Registry registry, List<ProgressObserver> observers) {
    super(bigg, parameters, registry, observers);
  }

  @Override
  public void annotate(List<Compartment> compartments) throws SQLException {
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
  @Override
  public void annotate(Compartment compartment) throws SQLException {
    BiGGId biggId = new BiGGId(compartment.getId());
    if (bigg.isCompartment(biggId.getAbbreviation())) {
      compartment.addCVTerm(
              new CVTerm(CVTerm.Qualifier.BQB_IS,
                      new IdentifiersOrgURI("bigg.compartment", biggId).getURI()));
      compartment.setSBOTerm(SBO.getCompartment()); // physical compartment
      if (!compartment.isSetName() || compartment.getName().equals("default")) {
        bigg.getCompartmentName(biggId).ifPresent(compartment::setName);
      }
    }
  }
}