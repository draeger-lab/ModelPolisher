package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.annotation.IAnnotateSBases;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDB;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import de.uni_halle.informatik.biodata.mp.resolver.Registry;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrgURI;
import org.sbml.jsbml.*;
import org.sbml.jsbml.CVTerm.Qualifier;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static de.uni_halle.informatik.biodata.mp.db.bigg.BiGGDBContract.Constants.TYPE_SPECIES;
import static java.text.MessageFormat.format;

/**
 * This class provides functionality to annotate a species in an SBML model using BiGG database identifiers.
 * It extends the {@link BiGGCVTermAnnotator} class, allowing it to manage controlled vocabulary (CV) terms
 * associated with the species. The class handles various aspects of species annotation including setting
 * the species' name, SBO term, and additional annotations. It also sets the chemical formula and charge
 * for the species using FBC (Flux Balance Constraints) extensions.
 */
public class BiGGSpeciesAnnotator extends BiGGCVTermAnnotator<Species> implements IAnnotateSBases<Species> {

  private final SBOParameters sboParameters;

  protected BiGGSpeciesAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, SBOParameters sboParameters, Registry registry) {
    super(bigg, parameters, registry);
      this.sboParameters = sboParameters;
  }
  protected BiGGSpeciesAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, SBOParameters sboParameters, Registry registry, List<ProgressObserver> observers) {
    super(bigg, parameters, registry, observers);
      this.sboParameters = sboParameters;
  }

  /**
   * Delegates annotation processing for all chemical species contained in the {@link Model}.
   * This method iterates over each species in the model and applies specific annotations.
   */
  @Override
  public void annotate(List<Species> species) throws SQLException {
    for (Species s : species) {
      statusReport("Annotating Species (3/5)  ", s);
      annotate(s);
    }
  }

  /**
   * This method annotates a species with various details fetched from the BiGG Knowledgebase. It performs the following:
   * 1. Sets the species name based on the BiGGId. If the species does not have a name, it uses the BiGGId as the name.
   * 2. Assigns an SBO (Systems Biology Ontology) term to the species based on the BiGGId.
   * 3. Adds additional annotations to the species, such as database cross-references.
   * <p>
   * The BiGGId used for these operations is either derived from the species' URI list or directly from its ID if available.
   */
  @Override
  public void annotate(Species species) throws SQLException {
    // Retrieve the BiGGId for the species, either from its URI list or its direct ID
    var biGGId = findBiGGId(species);
    setName(species, biGGId); // Set the species name based on the BiGGId
    addAnnotations(species, biGGId); // Add database cross-references and other annotations
  }


  /**
   * Validates the species ID and attempts to retrieve a corresponding BiGGId based on existing annotations.
   * This method first tries to create a BiGGId from the species ID. If the species ID does not correspond to a known
   * BiGGId in the database, it then searches through the species' annotations to find a valid BiGGId.
   *
   * @return An {@link Optional} containing the BiGGId if a valid one is found or created, otherwise {@link Optional#empty()}
   */
  @Override
  public BiGGId findBiGGId(Species species) throws SQLException {
    // Attempt to create a BiGGId from the species ID
    var metaboliteId = BiGGId.createMetaboliteId(species.getId());

    // Check if the created BiGGId is valid, if not, try to find a BiGGId from annotations
    boolean isBiGGid = bigg.isMetabolite(metaboliteId.getAbbreviation());

    if (!isBiGGid) {
      // Collect all resources from CVTerms that qualify as BQB_IS
      // Attempt to retrieve a BiGGId from the collected resources
      List<String> resources = species.getAnnotation().getListOfCVTerms()
              .stream()
              .filter(cvTerm -> cvTerm.getQualifier() == Qualifier.BQB_IS)
              .flatMap(term -> term.getResources().stream())
              .toList();
      var biggIdFromResources = getBiGGIdFromResources(resources, TYPE_SPECIES);
      if (biggIdFromResources.isPresent()) {
        return biggIdFromResources.get();
      }
    }
    return metaboliteId;
  }


  /**
   * Updates the name of the species based on data retrieved from the BiGG Knowledgebase.
   * The species name is set only if it has not been previously set or if the current name
   * follows a default format that combines the BiGGId abbreviation and
   * compartment code. This method relies on the availability of a valid {@link BiGGId} for the species.
   *
   * @param biggId The {@link BiGGId} associated with the species, used to fetch the component name from the BiGG database.
   */
  public void setName(Species species, BiGGId biggId) throws SQLException {
    if (!species.isSetName()
      || species.getName().equals(format("{0}_{1}", biggId.getAbbreviation(), biggId.getCompartmentCode()))) {
      bigg.getComponentName(biggId).ifPresent(species::setName);
    }
  }


  void addAnnotations(Species species, BiGGId biggId) throws IllegalArgumentException, SQLException {

    CVTerm cvTerm = null;
    for (CVTerm term : species.getAnnotation().getListOfCVTerms()) {
      if (term.getQualifier() == Qualifier.BQB_IS) {
        cvTerm = term;
        species.removeCVTerm(term);
        break;
      }
    }
    if (cvTerm == null) {
      cvTerm = new CVTerm(Qualifier.BQB_IS);
    }

    Set<String> annotations = new HashSet<>();

    boolean isBiGGMetabolite = bigg.isMetabolite(biggId.getAbbreviation());
    // using BiGG Database
    if (isBiGGMetabolite) {
      if (cvTerm.getResources().stream().noneMatch(resource -> resource.contains("bigg.metabolite"))) {
          annotations.add(new IdentifiersOrgURI("bigg.metabolite", biggId).getURI());
      }
      Set<String> biggAnnotations = bigg.getResources(biggId, biGGAnnotationParameters.includeAnyURI(), false)
              .stream()
              .map(IdentifiersOrgURI::getURI)
              .collect(Collectors.toSet());
      annotations.addAll(biggAnnotations);
    }

    // don't add resources that are already present
    annotations.removeAll(new HashSet<>(cvTerm.getResources()));
    // adding annotations to cvTerm
    List<String> sortedAnnotations = new ArrayList<>(annotations);
    Collections.sort(sortedAnnotations);
    for (String annotation : sortedAnnotations) {
      cvTerm.addResource(annotation);
    }
    if (cvTerm.getResourceCount() > 0) {
      species.addCVTerm(cvTerm);
    }
    if ((species.getCVTermCount() > 0) && !species.isSetMetaId()) {
      species.setMetaId(species.getId());
    }
  }

}
