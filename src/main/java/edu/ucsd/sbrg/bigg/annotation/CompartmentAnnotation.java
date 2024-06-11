package edu.ucsd.sbrg.bigg.annotation;

import edu.ucsd.sbrg.miriam.Registry;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.SBO;

import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.db.QueryOnce;

/**
 * This class is responsible for annotating a specific compartment within an SBML model using data from the BiGG database.
 * It allows for the addition of both BiGG and SBO annotations to a compartment, and can also set the compartment's name
 * based on information retrieved from the BiGG database.
 */
public class CompartmentAnnotation {
  /**
   * The compartment instance that will be annotated.
   */
  private final Compartment compartment;

  /**
   * Constructs a new CompartmentAnnotation object for a given compartment.
   * 
   * @param compartment The compartment to be annotated.
   */
  public CompartmentAnnotation(Compartment compartment) {
    this.compartment = compartment;
  }
  

  /**
   * Annotates the compartment with BiGG and SBO terms. If the compartment's name is not set or is set to "default",
   * it updates the name based on the BiGG database. This method only processes compartments that are recognized
   * within the BiGG Knowledgebase.
   */
  public void annotate() {
    BiGGId biggId = new BiGGId(compartment.getId());
    if (QueryOnce.isCompartment(biggId.getAbbreviation())) {
      compartment.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, Registry.createURI("bigg.compartment", biggId)));
      compartment.setSBOTerm(SBO.getCompartment()); // physical compartment
      if (!compartment.isSetName() || compartment.getName().equals("default")) {
        BiGGDB.getCompartmentName(biggId).ifPresent(compartment::setName);
      }
    }
  }
}
