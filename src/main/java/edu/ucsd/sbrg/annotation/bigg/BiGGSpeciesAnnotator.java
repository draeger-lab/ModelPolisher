package edu.ucsd.sbrg.annotation.bigg;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.annotation.IAnnotateSBases;
import edu.ucsd.sbrg.logging.BundleNames;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrgURI;
import org.sbml.jsbml.*;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.db.bigg.BiGGDBContract.Constants.TYPE_SPECIES;
import static java.text.MessageFormat.format;

/**
 * This class provides functionality to annotate a species in an SBML model using BiGG database identifiers.
 * It extends the {@link BiGGCVTermAnnotator} class, allowing it to manage controlled vocabulary (CV) terms
 * associated with the species. The class handles various aspects of species annotation including setting
 * the species' name, SBO term, and additional annotations. It also sets the chemical formula and charge
 * for the species using FBC (Flux Balance Constraints) extensions.
 */
public class BiGGSpeciesAnnotator extends BiGGCVTermAnnotator<Species> implements IAnnotateSBases<Species> {

  private static final Logger logger = LoggerFactory.getLogger(BiGGSpeciesAnnotator.class);
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.BIGG_ANNOTATION_MESSAGES);

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
   * 4. Sets the chemical formula and charge for the species using FBC (Flux Balance Constraints) extensions.
   * <p>
   * The BiGGId used for these operations is either derived from the species' URI list or directly from its ID if available.
   */
  @Override
  public void annotate(Species species) throws SQLException {
    // Retrieve the BiGGId for the species, either from its URI list or its direct ID
    var biGGId = findBiGGId(species);
    setName(species, biGGId); // Set the species name based on the BiGGId
    setSBOTerm(species, biGGId); // Assign the appropriate SBO term
    addAnnotations(species, biGGId); // Add database cross-references and other annotations
    FBCSetFormulaCharge(species, biGGId); // Set the chemical formula and charge
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
      var biggIdFromResources = getBiGGIdFromResources(
              species.getAnnotation().getListOfCVTerms()
                      .stream()
                      .filter(cvTerm -> cvTerm.getQualifier() == Qualifier.BQB_IS)
                      .flatMap(term -> term.getResources().stream())
                      .toList(),
              TYPE_SPECIES);
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


  /**
   * Assigns the SBO term to a species based on its component type as determined from the BiGG database.
   * The component type can be a metabolite, protein, or a generic material entity. If the component type is not explicitly
   * identified in the BiGG database, the species is annotated as a generic material entity unless the configuration
   * explicitly omits such generic terms (controlled by {@link SBOParameters#addGenericTerms()}).
   *
   * @param biggId The {@link BiGGId} associated with the species, used to determine the component type from the BiGG database.
   */
  private void setSBOTerm(Species species, BiGGId biggId) throws SQLException {
    bigg.getComponentType(biggId).ifPresentOrElse(type -> {
      switch (type) {
      case "metabolite":
        species.setSBOTerm(SBO.getSimpleMolecule()); // Assign SBO term for simple molecules (metabolites).
        break;
      case "protein":
        species.setSBOTerm(SBO.getProtein()); // Assign SBO term for proteins.
        break;
      default:
        if (sboParameters.addGenericTerms()) {
          species.setSBOTerm(SBO.getMaterialEntity()); // Assign SBO term for generic material entities.
        }
        break;
      }
    }, () -> {
      if (sboParameters.addGenericTerms()) {
        species.setSBOTerm(SBO.getMaterialEntity()); // Default SBO term assignment when no specific type is found.
      }
    });
  }


  void addAnnotations(Species species, BiGGId biggId) throws IllegalArgumentException, SQLException {

    // TODO: ???
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
      annotations.add(new IdentifiersOrgURI("bigg.metabolite", biggId).getURI());

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

  /**
   * Sets the chemical formula and charge for a species based on the provided BiGGId.
   * This method first checks if the species belongs to a BiGG model and retrieves the compartment code.
   * It then attempts to set the chemical formula if it has not been set already. The formula is fetched
   * from the BiGG database either based on the model ID or the compartment code if the model ID fetch fails.
   * If the formula is successfully retrieved, it is set using the FBCSpeciesPlugin.
   * Similarly, the charge is fetched and set if the species does not already have a charge set.
   * If a charge is fetched and if it contradicts an existing charge, log a warning and unset the existing charge.
   *
   * @param biggId: {@link BiGGId} from species id
   */
  @SuppressWarnings("deprecation")
  private void FBCSetFormulaCharge(Species species, BiGGId biggId) throws SQLException {
    boolean isBiGGModel = species.getModel() !=null && bigg.isModel(species.getModel().getId());

    String compartmentCode = biggId.getCompartmentCode();
    FBCSpeciesPlugin fbcSpecPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
    boolean compartmentNonEmpty = compartmentCode != null && !compartmentCode.isEmpty();
    if (!fbcSpecPlug.isSetChemicalFormula()) {
      Optional<String> chemicalFormula = Optional.empty();
      if (isBiGGModel) {
        chemicalFormula = bigg.getChemicalFormula(biggId.getAbbreviation(), species.getModel().getId());
      }
      if ((!isBiGGModel || chemicalFormula.isEmpty()) && compartmentNonEmpty) {
        chemicalFormula = bigg.getChemicalFormulaByCompartment(biggId.getAbbreviation(), compartmentCode);
      }
        chemicalFormula.ifPresent(fbcSpecPlug::setChemicalFormula);
    }
    Optional<Integer> chargeFromBiGG = Optional.empty();
    if (isBiGGModel) {
      chargeFromBiGG = bigg.getCharge(biggId.getAbbreviation(), species.getModel().getId());
    } else if (compartmentNonEmpty) {
      chargeFromBiGG = bigg.getChargeByCompartment(biggId.getAbbreviation(), biggId.getCompartmentCode());
    }
    if (species.isSetCharge()) {
      chargeFromBiGG
              .filter(charge -> charge != species.getCharge())
              .ifPresent(charge ->
                      logger.debug(format(MESSAGES.getString("CHARGE_CONTRADICTION"),
                              charge, species.getCharge(), species.getId())));
      species.unsetCharge();
    }
    chargeFromBiGG.filter(charge -> charge != 0).ifPresent(fbcSpecPlug::setCharge);
  }


}
