package edu.ucsd.sbrg.polishing.ext.fbc;

import edu.ucsd.sbrg.polishing.AbstractPolisher;
import edu.ucsd.sbrg.polishing.AnnotationPolisher;
import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.polishing.IPolishSBases;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import edu.ucsd.sbrg.db.bigg.BiGGId;
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
      statusReport("Polishing Gene Products (8/9)  ", geneProduct);
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
