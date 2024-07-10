package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.db.MemorizedQuery;
import edu.ucsd.sbrg.miriam.Registry;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.GeneProduct;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for annotating an SBML {@link Model} with relevant metadata and references.
 * It handles the annotation of the model itself and delegates the annotation of contained elements such as
 * {@link Compartment}, {@link Species}, {@link Reaction}, and {@link GeneProduct}.
 * The annotations can include taxonomy information, database references, and meta identifiers.
 */
public class ModelAnnotation {

  private final Model model;
  private final Parameters parameters;

  public ModelAnnotation(Model model, Parameters parameters) {
    this.model = model;
    this.parameters = parameters;
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
  public void annotate() {
    // Retrieve the model's identifier
    String id = model.getId();
    // Attempt to fetch and add a taxonomy annotation using the model's ID
    BiGGDB.getTaxonId(id).ifPresent(
      taxonId -> model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_HAS_TAXON, Registry.createURI("taxonomy", taxonId))));
    // Check if the model is recognized in the database and add specific annotations
    if (MemorizedQuery.isModel(id)) {
      addBiGGModelAnnotations();
    }
    // Set the model's MetaId to its ID if MetaId is not set and there are existing CVTerms
    if (!model.isSetMetaId() && (model.getCVTermCount() > 0)) {
      model.setMetaId(model.getId());
    }
  }

  /**
   * Adds annotations related to the genomic sequence or assembly for models stored in the BiGG database.
   * This method first adds a MIRIAM annotation indicating the model's identity within BiGG.
   * It then attempts to annotate the model with its genomic accession number, which could be a RefSeq or a genome assembly accession.
   * If the accession matches a RefSeq pattern, it is annotated directly using a RefSeq URI.
   * If the accession matches a genome assembly pattern and non-MIRIAM URIs are allowed (controlled by {@link Parameters#includeAnyURI()}),
   * it is annotated using a direct link to the NCBI assembly resource. Otherwise, it uses a direct link to the NCBI nucleotide resource.
   * Annotations are only added if they are successfully created and contain at least one resource.
   */
  private void addBiGGModelAnnotations() {
    // Add a basic MIRIAM annotation indicating the model's identity within BiGG
    model.addCVTerm(new CVTerm(CVTerm.Qualifier.BQM_IS, Registry.createURI("bigg.model", model.getId())));

    // Retrieve the genomic accession number for the model
    String accession = BiGGDB.getGenomeAccesion(model.getId());

    // Prepare a pattern matcher for RefSeq accession numbers
    Matcher refseqMatcher = Pattern.compile("^(((AC|AP|NC|NG|NM|NP|NR|NT|NW|XM|XP|XR|YP|ZP)_\\d+)|(NZ_[A-Z]{2,4}\\d+))(\\.\\d+)?$")
                                   .matcher(accession);
    // Create a CVTerm for versioning annotation
    CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS_VERSION_OF);

    // Check if the accession matches the RefSeq pattern
    if (refseqMatcher.matches()) {
      // Add a RefSeq resource to the CVTerm
      term.addResource(Registry.createShortURI("refseq:" + accession));
    } else {
      // Check if non-MIRIAM URIs are allowed
      if (parameters.includeAnyURI()) {
        // Prepare a pattern matcher for genome assembly accession numbers
        Matcher genomeAssemblyMatcher = Pattern.compile("^GC[AF]_[0-9]{9}\\.[0-9]+$").matcher(accession);
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
