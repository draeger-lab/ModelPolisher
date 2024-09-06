package de.uni_halle.informatik.biodata.mp.polishing.ext.fbc;

import de.uni_halle.informatik.biodata.mp.polishing.AbstractPolisher;
import de.uni_halle.informatik.biodata.mp.polishing.AnnotationPolisher;
import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.polishing.IPolishSBases;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GeneProductsPolisher extends AbstractPolisher implements IPolishSBases<GeneProduct> {
  private static final Logger logger = LoggerFactory.getLogger(GeneProductsPolisher.class);

  public GeneProductsPolisher(PolishingParameters parameters, Registry registry, List<ProgressObserver> observers) {
      super(parameters, registry, observers);
  }


  @Override
  public void polish(List<GeneProduct> geneProducts) {
    logger.debug("Polish Gene Products");

    for (GeneProduct geneProduct : geneProducts) {
      statusReport("Polishing Gene Products (9/9)  ", geneProduct);
      polish(geneProduct);
    }
  }

  @Override
  public void polish(GeneProduct geneProduct) {
    // Process the annotations associated with the gene product
    new AnnotationPolisher(polishingParameters, registry).polish(geneProduct.getAnnotation());

    setIdToBiggId(geneProduct);

    if ((geneProduct.getCVTermCount() > 0) && !geneProduct.isSetMetaId()) {
      geneProduct.setMetaId(geneProduct.getId());
    }

    setName(geneProduct);
  }

  private void setName(GeneProduct geneProduct) {
    if (!geneProduct.isSetName() || geneProduct.getName().equalsIgnoreCase("None")) {
      if (!geneProduct.getLabel().equalsIgnoreCase("None")) {
        geneProduct.setName(geneProduct.getLabel());
      } else {
        geneProduct.setName(geneProduct.getId());
      }
    }
  }

  private void setIdToBiggId(GeneProduct geneProduct) {
    // Create a new BiGG ID for the gene product, if possible
    var biggId = BiGGId.createGeneId(geneProduct.getId());
    String id = biggId.toBiGGId();
    geneProduct.setId(id);
  }

}
