package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.identifiersorg.IdentifiersOrg;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for annotating an SBML {@link Model} with relevant metadata and references.
 * It handles the annotation of the model itself and delegates the annotation of contained elements such as
 * {@link Compartment}, {@link Species}, {@link Reaction}, and {@link GeneProduct}.
 * The annotations can include taxonomy information, database references, and meta identifiers.
 */
public class ModelAnnotator extends AbstractAnnotator<Model>{

  public static final String REF_SEQ_ACCESSION_NUMBER_PATTERN = "^(((AC|AP|NC|NG|NM|NP|NR|NT|NW|XM|XP|XR|YP|ZP)_\\d+)|(NZ_[A-Z]{2,4}\\d+))(\\.\\d+)?$";
  public static final String GENOME_ASSEMBLY_ID_PATTERN = "^GC[AF]_[0-9]{9}\\.[0-9]+$";

  public ModelAnnotator(Parameters parameters) {
    super(parameters);
  }
  public ModelAnnotator(Parameters parameters, List<ProgressObserver> observers) {
    super(parameters, observers);
  }


  /**
   * Annotates the {@link Model} with relevant metadata and delegates the annotation of contained elements such as
   * {@link Compartment}, {@link Species}, {@link Reaction}, and {@link GeneProduct}.
   * 
   * Steps:
   * 1. Retrieves the model's ID and uses it to fetch and add a taxonomy annotation if available.
   * 2. Checks if the model exists in the database and adds specific BiGG database annotations.
   * 3. Sets the model's MetaId to its ID if MetaId is not already set and the model has at least one CVTerm.
   */
  @Override
  public void annotate(Model model) {
    // Retrieve the model ID
    String id = model.getId();
    // Attempt to retrieve the organism name associated with the model ID; use an empty string if not available
    String organism = BiGGDB.getOrganism(id).orElse("");
    if (!model.isSetName()) {
      model.setName(organism);
    }

    addTaxonomyAnnotation(model, model.getId());

    // annotation indicating the model's identity within BiGG
    model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS, IdentifiersOrg.createURI("bigg.model", model.getId())));

    // Retrieve the genomic accession number for the model
    String accession = BiGGDB.getGenomeAccesion(model.getId());
    addNCBIReferenceAnnotation(model, accession);

    // Set the model's MetaId to its ID if MetaId is not set and there are existing CVTerms
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }
  }


  private void addTaxonomyAnnotation(Model model, String modelId) {
    // Attempt to fetch and add a taxonomy annotation using the model's ID
    BiGGDB.getTaxonId(modelId).ifPresent(
      taxonId -> model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON,
              IdentifiersOrg.createURI("taxonomy", taxonId))));
  }


  private void addNCBIReferenceAnnotation(Model model, String accession) {
    // Prepare a pattern matcher for RefSeq accession numbers
    Matcher refseqMatcher = Pattern.compile(REF_SEQ_ACCESSION_NUMBER_PATTERN).matcher(accession);
    // Create a CVTerm for versioning annotation
    CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF);
    // Check if the accession matches the RefSeq pattern
    if (refseqMatcher.matches()) {
      // Add a RefSeq resource to the CVTerm
      term.addResource(IdentifiersOrg.createShortURI("refseq:" + accession));
    } else {
      // Check if non-MIRIAM URIs are allowed
      if (parameters.includeAnyURI()) {
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
