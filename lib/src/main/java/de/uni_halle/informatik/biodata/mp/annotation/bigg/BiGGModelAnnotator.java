package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.annotation.IAnnotateSBases;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDB;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrgURI;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for annotating an SBML {@link Model} with relevant metadata and references.
 * It handles the annotation of the model itself and delegates the annotation of contained elements such as
 * {@link Compartment}, {@link Species}, {@link Reaction}, and {@link GeneProduct}.
 * The annotations can include taxonomy information, database references, and meta identifiers.
 */
public class BiGGModelAnnotator extends AbstractBiGGAnnotator implements IAnnotateSBases<Model> {

  public static final String REF_SEQ_ACCESSION_NUMBER_PATTERN = "^(((AC|AP|NC|NG|NM|NP|NR|NT|NW|XM|XP|XR|YP|ZP)_\\d+)|(NZ_[A-Z]{2,4}\\d+))(\\.\\d+)?$";
  public static final String GENOME_ASSEMBLY_ID_PATTERN = "^GC[AF]_[0-9]{9}\\.[0-9]+$";

  public BiGGModelAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, Registry registry) {
    super(bigg, parameters, registry);
  }
  public BiGGModelAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, Registry registry, List<ProgressObserver> observers) {
    super(bigg, parameters, registry, observers);
  }


  /**
   * Annotates the {@link Model} with relevant metadata and delegates the annotation of contained elements such as
   * {@link Compartment}, {@link Species}, {@link Reaction}, and {@link GeneProduct}.
   * <p>
   * Steps:
   * 1. Retrieves the model's ID and uses it to fetch and add a taxonomy annotation if available.
   * 2. Checks if the model exists in the database and adds specific BiGG database annotations.
   * 3. Sets the model's MetaId to its ID if MetaId is not already set and the model has at least one CVTerm.
   */
  @Override
  public void annotate(Model model) throws SQLException {
    // Retrieve the model ID
    String id = model.getId();
    // Attempt to retrieve the organism name associated with the model ID; use an empty string if not available
    String organism = bigg.getOrganism(id).orElse("");
    if (!model.isSetName()) {
      model.setName(organism);
    }

    addTaxonomyAnnotation(model, model.getId());

    // annotation indicating the model's identity within BiGG
    model.addCVTerm(
            new CVTerm(CVTerm.Qualifier.BQM_IS,
            new IdentifiersOrgURI("bigg.model", model.getId()).getURI()));

    // Retrieve the genomic accession number for the model
    String accession = bigg.getGenomeAccesion(model.getId());
    addNCBIReferenceAnnotation(model, accession);

    // Set the model's MetaId to its ID if MetaId is not set and there are existing CVTerms
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }
  }


  private void addTaxonomyAnnotation(Model model, String modelId) throws SQLException {
    // Attempt to fetch and add a taxonomy annotation using the model's ID
    bigg.getTaxonId(modelId).ifPresent(
      taxonId -> model.addCVTerm(
              new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON,
                      new IdentifiersOrgURI("taxonomy", taxonId).getURI())));
  }


  private void addNCBIReferenceAnnotation(Model model, String accession) {
    // Prepare a pattern matcher for RefSeq accession numbers
    Matcher refseqMatcher = Pattern.compile(REF_SEQ_ACCESSION_NUMBER_PATTERN).matcher(accession);
    // Create a CVTerm for versioning annotation
    CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF);
    // Check if the accession matches the RefSeq pattern
    if (refseqMatcher.matches()) {
      // Add a RefSeq resource to the CVTerm
      term.addResource(new IdentifiersOrgURI("refseq", accession).getURI());
    } else {
      // Check if non-MIRIAM URIs are allowed
      if (biGGAnnotationParameters.includeAnyURI()) {
        // Prepare a pattern matcher for genome assembly accession numbers
        Matcher genomeAssemblyMatcher = Pattern.compile(GENOME_ASSEMBLY_ID_PATTERN).matcher(accession);
        if (genomeAssemblyMatcher.matches()) {
          // Add a genome assembly resource to the CVTerm, resolving non-MIRIAM way due to known issues
          term.addResource("https://www.ncbi.nlm.nih.gov/assembly/" + accession);
        } else {
          // Add a nucleotide resource to the CVTerm for other cases
          term.addResource("https://www.ncbi.nlm.nih.gov/nuccore/" + accession);
        }
      }
    }
    // Add the CVTerm to the model if it contains any resources
    if (term.getResourceCount() > 0) {
      model.addCVTerm(term);
    }
  }

}
