package de.uni_halle.informatik.biodata.mp.annotation.bigg.ext.fbc;

import de.zbit.util.ResourceManager;
import de.uni_halle.informatik.biodata.mp.annotation.IAnnotateSBases;
import de.uni_halle.informatik.biodata.mp.logging.BundleNames;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.annotation.bigg.BiGGCVTermAnnotator;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDB;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDBContract.Constants.TYPE_GENE_PRODUCT;
import static java.text.MessageFormat.format;

/**
 * Provides functionality to annotate gene products in an SBML model using data from the BiGG database.
 * This class extends {@link BiGGCVTermAnnotator} and specifically handles the annotation of {@link GeneProduct} instances.
 * It includes methods to validate gene product IDs, retrieve and set labels, and add annotations based on BiGG IDs.
 */
public class BiGGGeneProductAnnotator extends BiGGCVTermAnnotator<GeneProduct> implements IAnnotateSBases<GeneProduct> {

  private static final Logger logger = LoggerFactory.getLogger(BiGGGeneProductAnnotator.class);
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.BIGG_ANNOTATION_MESSAGES);
  public static final String BIGG_GENE_ID_PATTERN = "^(G_)?([a-zA-Z][a-zA-Z0-9_]+)(?:_([a-z][a-z0-9]?))?(?:_([A-Z][A-Z0-9]?))?$";

  /**
   * Instance of gene product to annotate
   */
  private final BiGGGeneProductReferencesAnnotator gprAnnotator;

  public BiGGGeneProductAnnotator(BiGGGeneProductReferencesAnnotator gprAnnotator, BiGGDB bigg, BiGGAnnotationParameters parameters,
                                  Registry registry, List<ProgressObserver> observers) {
    super(bigg, parameters, registry, observers);
    this.gprAnnotator = gprAnnotator;
  }


  /**
   * This method handles the annotation of gene products in a given SBML model. It checks if the model has the FBC plugin
   * set and then proceeds to annotate each gene product found within the model. The progress bar is updated to reflect
   * the number of gene products being annotated.
   */
  @Override
  public void annotate(List<GeneProduct> geneProducts) throws SQLException {
    // Iterate over each gene product and annotate it
    for (GeneProduct geneProduct : geneProducts) {
      statusReport("Annotating Gene Products (5/5)  ", geneProduct);
      annotate(geneProduct);
    }

  }

  /**
   * Annotates a gene product by adding relevant metadata and references.
   * This method first checks the gene product's ID for validity and retrieves a corresponding BiGGId if available.
   * It then attempts to get a label for the gene product. If no label is found, the method returns early.
   * If a label is present, it updates the gene product reference in the association, adds annotations using the BiGGId,
   * and sets the gene product's metaId if it has any CV terms. Finally, it sets the gene product's label name.
   */
  @Override
  public void annotate(GeneProduct geneProduct) throws SQLException {
    var biggId = findBiGGId(geneProduct);

    String label = getLabel(geneProduct, biggId);
    if (label.isEmpty()) {
      return;
    }

    gprAnnotator.update(geneProduct);
    addAnnotations(geneProduct, biggId);
    if (geneProduct.getCVTermCount() > 0) {
      geneProduct.setMetaId(biggId.toBiGGId());
    }
    setGPLabelName(geneProduct, label);
  }



  /**
   * Validates the ID of a {@link GeneProduct} against the expected BiGG ID format and attempts to retrieve a
   * corresponding {@link BiGGId} from existing annotations if the initial ID does not conform to the BiGG format.
   * The method first checks if the gene product's ID matches the BiGG ID pattern. If it does not match, it then
   * tries to find a valid BiGG ID from the gene product's annotations. If a valid BiGG ID is found among the annotations,
   * it updates the ID; otherwise, it retains the original ID.
   *
   * @return An {@link Optional<BiGGId>} containing the validated or retrieved BiGG ID, or an empty Optional if no valid ID is found.
   */
  @Override
  public BiGGId findBiGGId(GeneProduct geneProduct) throws SQLException {
    String id = geneProduct.getId();
    boolean isBiGGid = id.matches(BIGG_GENE_ID_PATTERN);
    if (!isBiGGid) {
      // Collect all resources from CVTerms that qualify as BQB_IS into a list
      List<String> resources = geneProduct.getAnnotation().getListOfCVTerms().stream()
                                          .filter(cvTerm -> cvTerm.getQualifier() == Qualifier.BQB_IS)
                                          .flatMap(term -> term.getResources().stream())
                                          .collect(Collectors.toList());
      // Attempt to update the ID with a valid BiGG ID from the resources, if available
      Optional<BiGGId> biGGIdFromResources = getBiGGIdFromResources(resources, TYPE_GENE_PRODUCT);
      if (biGGIdFromResources.isPresent()) {
        return biGGIdFromResources.get();
      }
    }
    // Create and return a BiGGId object based on the validated or updated ID
    return BiGGId.createGeneId(id);
  }


  /**
   * Retrieves the label for a gene product based on the provided BiGGId. If the gene product has a label set and it is not "None",
   * that label is returned. If no label is set but the gene product has an ID, the BiGGId is converted to a string and returned.
   * If neither condition is met, an empty string is returned.
   *
   * @param biggId An Optional containing the BiGGId of the gene product, which may be used to generate a label if the gene product's own label is not set.
   * @return An {@code Optional<String>} containing the label of the gene product, or an empty string if no appropriate label is found.
   */
  public String getLabel(GeneProduct geneProduct, BiGGId biggId) {
    if (geneProduct.isSetLabel() && !geneProduct.getLabel().equalsIgnoreCase("None")) {
      return geneProduct.getLabel();
    } else if (geneProduct.isSetId()) {
      return biggId.toBiGGId();
    } else {
      return "";
    }
  }

  
  /**
   * Updates the label of a gene product and sets its name based on the retrieved gene name from the BiGG database.
   * If the current label is set to "None", it updates the label to the provided one. It then attempts to fetch
   * the gene name corresponding to this label from the BiGG database. If a gene name is found, it checks if the
   * current gene product name is different from the fetched name. If they differ, it logs a warning and updates
   * the gene product name. If no gene name is found, it logs this as a fine-level message.
   *
   * @param label The label to set or use for fetching the gene name. This label should correspond to a {@link BiGGId}
   *              or be derived from {@link GeneProduct#getId()}.
   */
  public void setGPLabelName(GeneProduct geneProduct, String label) throws SQLException {
    // Check if the current label is "None" and update it if so
    if (geneProduct.getLabel().equalsIgnoreCase("None")) {
      geneProduct.setLabel(label);
    }
    // Attempt to fetch the gene name from the BiGG database using the label
    bigg.getGeneName(label).ifPresent(geneName -> {
      // Log if no gene name is associated with the label
      if (geneName.isEmpty()) {
        logger.debug(format(MESSAGES.getString("NO_GENE_FOR_LABEL"), geneProduct.getName()));
      } else {
        // Log a warning if the gene product name is set and differs from the fetched gene name
        if (geneProduct.isSetName() && !geneProduct.getName().equals(geneName)) {
          logger.debug(format(MESSAGES.getString("UPDATE_GP_NAME"), geneProduct.getName(), geneName));
        }
        // Update the gene product name with the fetched gene name
        geneProduct.setName(geneName);
      }
    });
  }


  /**
   * Adds annotations to a gene product based on a given {@link BiGGId}. This method differentiates between
   * annotations that specify what the gene product 'is' and what it 'is encoded by'. Resources are fetched
   * from the BiGG database using the abbreviation from the provided BiGGId. Each resource URL is checked and
   * parsed to determine the appropriate category ('is' or 'is encoded by') based on predefined prefixes.
   *
   * @param biggId The {@link BiGGId} associated with the gene product, typically derived from a species ID.
   */
  public void addAnnotations(GeneProduct geneProduct, BiGGId biggId) throws SQLException {
    CVTerm termIs = new CVTerm(Qualifier.BQB_IS);
    CVTerm termEncodedBy = new CVTerm(Qualifier.BQB_IS_ENCODED_BY);
    // Retrieve gene IDs from BiGG database and categorize them based on their prefix
    bigg.getGeneIds(biggId.getAbbreviation()).forEach(
            uri -> {
              switch (IdentifiersOrg.fixIdentifiersOrgUri(uri).getPrefix()) {
                case "interpro":
                case "pdb":
                case "uniprot":
                  termIs.addResource(uri.getURI());
                  break;
                default:
                  termEncodedBy.addResource(uri.getURI());
              }
            });
    // Add the CVTerm to the gene product if resources are present
    if (termIs.getResourceCount() > 0) {
      geneProduct.addCVTerm(termIs);
    }
    if (termEncodedBy.getResourceCount() > 0) {
      geneProduct.addCVTerm(termEncodedBy);
    }
  }
}
