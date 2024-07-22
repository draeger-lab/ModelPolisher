package edu.ucsd.sbrg.polishing.fbc;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.polishing.AbstractPolisher;
import edu.ucsd.sbrg.polishing.AnnotationPolisher;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import edu.ucsd.sbrg.db.bigg.BiGGId;

import java.util.List;

/**
 * This class is responsible for polishing GeneProduct instances by processing their annotations and adjusting their identifiers and names.
 */
public class GeneProductsPolisher extends AbstractPolisher<GeneProduct> {


  public GeneProductsPolisher(Parameters parameters, Registry registry, List<ProgressObserver> observers) {
      super(parameters, registry, observers);
  }


  @Override
  public void polish(List<GeneProduct> geneProducts) {
    for (GeneProduct geneProduct : geneProducts) {
      statusReport("Polishing Gene Products (8/9)  ", geneProduct);
      polish(geneProduct);
    }
  }

  /**
   * Polishes the GeneProduct by processing its annotations, setting its ID and name based on certain conditions.
   * The method first processes the annotations of the GeneProduct. It then determines a suitable label for the
   * GeneProduct based on its existing label or ID. If no suitable label is found, the method returns early.
   * If a new BiGG ID is generated and differs from the current ID, it updates the GeneProduct's ID and potentially
   * its metaId if CV terms are present. Finally, if the GeneProduct does not have a name or its name is "None",
   * it sets the GeneProduct's name to the determined label.
   */
  @Override
  public void polish(GeneProduct geneProduct) {
    // Process the annotations associated with the gene product
    new AnnotationPolisher(parameters, registry).polish(geneProduct.getAnnotation());

    setName(geneProduct);

    setIdToBiggId(geneProduct);
  }

  private void setName(GeneProduct geneProduct) {
    String label = null;
    // Determine the label from the gene product's label or ID
    if (geneProduct.isSetLabel() && !geneProduct.getLabel().equalsIgnoreCase("None")) {
      label = geneProduct.getLabel();
    } else if (geneProduct.isSetId()) {
      label = geneProduct.getId();
    }

    if ((!geneProduct.isSetName() || geneProduct.getName().equalsIgnoreCase("None"))
            && label != null) {
      geneProduct.setName(label);
    }
  }

  private void setIdToBiggId(GeneProduct geneProduct) {
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
  }

}
