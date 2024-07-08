package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.miriam.Registry;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import edu.ucsd.sbrg.bigg.BiGGId;

/**
 * This class is responsible for polishing GeneProduct instances by processing their annotations and adjusting their identifiers and names.
 */
public class GeneProductPolishing {

  private final GeneProduct geneProduct;

  public GeneProductPolishing(GeneProduct geneProduct) {
    this.geneProduct = geneProduct;
  }


  /**
   * Polishes the GeneProduct by processing its annotations, setting its ID and name based on certain conditions.
   * The method first processes the annotations of the GeneProduct. It then determines a suitable label for the
   * GeneProduct based on its existing label or ID. If no suitable label is found, the method returns early.
   * If a new BiGG ID is generated and differs from the current ID, it updates the GeneProduct's ID and potentially
   * its metaId if CV terms are present. Finally, if the GeneProduct does not have a name or its name is "None",
   * it sets the GeneProduct's name to the determined label.
   */
  public void polish() {
    // Process the annotations associated with the gene product
    Registry.processResources(geneProduct.getAnnotation());
    
    // Initialize label variable
    String label = null;
    
    // Determine the label from the gene product's label or ID
    if (geneProduct.isSetLabel() && !geneProduct.getLabel().equalsIgnoreCase("None")) {
      label = geneProduct.getLabel();
    } else if (geneProduct.isSetId()) {
      label = geneProduct.getId();
    }
    
    // If no label is determined, exit the method
    if (label == null) {
      return;
    }
    
    // Create a new BiGG ID for the gene product, if possible
    BiGGId.createGeneId(geneProduct.getId()).ifPresent(biggId -> {
      String id = biggId.toBiGGId();
      
      // Update the gene product's ID if the new ID is different
      if (!id.equals(geneProduct.getId())) {
        geneProduct.setId(id);
      }
      
      // Set the metaId if there are CV terms associated with the gene product
      if (geneProduct.getCVTermCount() > 0) {
        geneProduct.setMetaId(id);
      }
    });
    
    // Set the gene product's name if it is not set or is "None"
    if (!geneProduct.isSetName() || geneProduct.getName().equalsIgnoreCase("None")) {
      geneProduct.setName(label);
    }
  }
}
