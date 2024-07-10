package edu.ucsd.sbrg.annotation;

import de.zbit.util.ResourceManager;
import de.zbit.util.Utils;
import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.db.MemorizedQuery;
import edu.ucsd.sbrg.polishing.PolishingUtils;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.SBO;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.annotation.BiGGAnnotation.getBiGGIdFromResources;
import static edu.ucsd.sbrg.db.bigg.BiGGDBContract.Constants.TYPE_SPECIES;
import static java.text.MessageFormat.format;

/**
 * This class provides functionality to annotate a species in an SBML model using BiGG database identifiers.
 * It extends the {@link CVTermAnnotation} class, allowing it to manage controlled vocabulary (CV) terms
 * associated with the species. The class handles various aspects of species annotation including setting
 * the species' name, SBO term, and additional annotations. It also sets the chemical formula and charge
 * for the species using FBC (Flux Balance Constraints) extensions.
 */
public class SpeciesAnnotation extends CVTermAnnotation {

  /**
   * A {@link Logger} for this class.
   */
  static final Logger logger = Logger.getLogger(SpeciesAnnotation.class.getName());
  /**
   * Localization support.
   */
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   * Instance of chemical species to annotate
   */
  private final Species species;
  private final Parameters parameters;

  public SpeciesAnnotation(Species species, Parameters parameters) {
    super(parameters);
    this.species = species;
    this.parameters = parameters;
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
  public void annotate() {
    // Retrieve the BiGGId for the species, either from its URI list or its direct ID
    checkId().ifPresent(biggId -> {
      setName(biggId); // Set the species name based on the BiGGId
      setSBOTerm(biggId); // Assign the appropriate SBO term
      addAnnotations(biggId); // Add database cross-references and other annotations
      FBCSetFormulaCharge(biggId); // Set the chemical formula and charge
    });
  }


  /**
   * Validates the species ID and attempts to retrieve a corresponding BiGGId based on existing annotations.
   * This method first tries to create a BiGGId from the species ID. If the species ID does not correspond to a known
   * BiGGId in the database, it then searches through the species' annotations to find a valid BiGGId.
   *
   * @return An {@link Optional} containing the BiGGId if a valid one is found or created, otherwise {@link Optional#empty()}
   */
  @Override
  public Optional<BiGGId> checkId() {
    // Attempt to create a BiGGId from the species ID
    Optional<BiGGId> metaboliteId = BiGGId.createMetaboliteId(species.getId());
    // Check if the created BiGGId is valid, if not, try to find a BiGGId from annotations
    Optional<String> id = metaboliteId.flatMap(biggId -> {
      boolean isBiGGid = MemorizedQuery.isMetabolite(biggId.getAbbreviation());
      List<String> resources = new ArrayList<>();
      if (!isBiGGid) {
        // Collect all resources from CVTerms that qualify as BQB_IS
        resources = species.getAnnotation().getListOfCVTerms().stream()
                           .filter(cvTerm -> cvTerm.getQualifier() == Qualifier.BQB_IS)
                           .flatMap(term -> term.getResources().stream())
                           .collect(Collectors.toList());
      }
      // Attempt to retrieve a BiGGId from the collected resources
      return getBiGGIdFromResources(resources, TYPE_SPECIES);
    });
    // Return the found BiGGId or the originally created one if no new ID was found
    return id.map(BiGGId::createMetaboliteId).orElse(metaboliteId);
  }


  /**
   * Updates the name of the species based on data retrieved from the BiGG Knowledgebase. The species name is set only if it
   * has not been previously set or if the current name follows a default format that combines the BiGGId abbreviation and
   * compartment code. This method relies on the availability of a valid {@link BiGGId} for the species.
   *
   * @param biggId The {@link BiGGId} associated with the species, used to fetch the component name from the BiGG database.
   */
  public void setName(BiGGId biggId) {
    if (!species.isSetName()
      || species.getName().equals(format("{0}_{1}", biggId.getAbbreviation(), biggId.getCompartmentCode()))) {
      BiGGDB.getComponentName(biggId).map(PolishingUtils::polishName).ifPresent(species::setName);
    }
  }


  /**
   * Assigns the SBO term to a species based on its component type as determined from the BiGG database.
   * The component type can be a metabolite, protein, or a generic material entity. If the component type is not explicitly
   * identified in the BiGG database, the species is annotated as a generic material entity unless the configuration
   * explicitly omits such generic terms (controlled by {@link Parameters#omitGenericTerms()}).
   *
   * @param biggId The {@link BiGGId} associated with the species, used to determine the component type from the BiGG database.
   */
  private void setSBOTerm(BiGGId biggId) {
    BiGGDB.getComponentType(biggId).ifPresentOrElse(type -> {
      switch (type) {
      case "metabolite":
        species.setSBOTerm(SBO.getSimpleMolecule()); // Assign SBO term for simple molecules (metabolites).
        break;
      case "protein":
        species.setSBOTerm(SBO.getProtein()); // Assign SBO term for proteins.
        break;
      default:
        if (!parameters.omitGenericTerms()) {
          species.setSBOTerm(SBO.getMaterialEntity()); // Assign SBO term for generic material entities.
        }
        break;
      }
    }, () -> {
      if (!parameters.omitGenericTerms()) {
        species.setSBOTerm(SBO.getMaterialEntity()); // Default SBO term assignment when no specific type is found.
      }
    });
  }


  /**
   * This method delegates the task of adding annotations to the species based on the provided {@link BiGGId}.
   * It ensures that annotations are added to the species, updates HTTP URIs to HTTPS in MIRIAM URIs, and merges any duplicate annotations.
   *
   * @param biggId the {@link BiGGId} associated with the species ID, used for fetching and adding annotations.
   */
  @Override
  public void addAnnotations(BiGGId biggId) {
    addAnnotations(species, biggId);
  }


  /**
   * Sets the chemical formula and charge for a species based on the provided BiGGId.
   * This method first checks if the species belongs to a BiGG model and retrieves the compartment code.
   * It then attempts to set the chemical formula if it has not been set already. The formula is fetched
   * from the BiGG database either based on the model ID or the compartment code if the model ID fetch fails.
   * If the formula is successfully retrieved, it is set using the FBCSpeciesPlugin.
   * Similarly, the charge is fetched and set if the species does not already have a charge set.
   * If a charge is fetched and it contradicts an existing charge, a warning is logged and the existing charge is unset.
   *
   * @param biggId: {@link BiGGId} from species id
   */
  @SuppressWarnings("deprecation")
  private void FBCSetFormulaCharge(BiGGId biggId) {
    boolean isBiGGModel = species.getModel() !=null && MemorizedQuery.isModel(species.getModel().getId());

    String compartmentCode = biggId.getCompartmentCode();
    FBCSpeciesPlugin fbcSpecPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
    boolean compartmentNonEmpty = compartmentCode != null && !compartmentCode.isEmpty();
    if (!fbcSpecPlug.isSetChemicalFormula()) {
      Optional<String> chemicalFormula = Optional.empty();
      if (isBiGGModel) {
        chemicalFormula = BiGGDB.getChemicalFormula(biggId.getAbbreviation(), species.getModel().getId());
      }
      if ((!isBiGGModel || chemicalFormula.isEmpty()) && compartmentNonEmpty) {
        chemicalFormula = BiGGDB.getChemicalFormulaByCompartment(biggId.getAbbreviation(), compartmentCode);
      }
      chemicalFormula.ifPresent(formula -> {
        try {
          fbcSpecPlug.setChemicalFormula(formula);
        } catch (IllegalArgumentException exc) {
          logger.severe(format(MESSAGES.getString("CHEM_FORMULA_INVALID"), Utils.getMessage(exc)));
        }
      });
    }
    Optional<Integer> chargeFromBiGG = Optional.empty();
    if (isBiGGModel) {
      chargeFromBiGG = BiGGDB.getCharge(biggId.getAbbreviation(), species.getModel().getId());
    } else if (compartmentNonEmpty) {
      chargeFromBiGG = BiGGDB.getChargeByCompartment(biggId.getAbbreviation(), biggId.getCompartmentCode());
    }
    if (species.isSetCharge()) {
      chargeFromBiGG.filter(charge -> charge != species.getCharge()).ifPresent(charge -> logger.warning(
        format(MESSAGES.getString("CHARGE_CONTRADICTION"), charge, species.getCharge(), species.getId())));
      species.unsetCharge();
    }
    chargeFromBiGG.filter(charge -> charge != 0).ifPresent(fbcSpecPlug::setCharge);
  }
}
