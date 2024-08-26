package edu.ucsd.sbrg.annotation.bigg.ext.fbc;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.annotation.IAnnotateSBases;
import edu.ucsd.sbrg.annotation.bigg.AbstractBiGGAnnotator;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.logging.BundleNames;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.validator.SyntaxChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

import static edu.ucsd.sbrg.db.bigg.BiGGDBContract.Constants.TYPE_SPECIES;
import static java.text.MessageFormat.format;

public class BiGGFBCSpeciesAnnotator extends AbstractBiGGAnnotator implements IAnnotateSBases<Species> {

    private static final Logger logger = LoggerFactory.getLogger(BiGGFBCSpeciesAnnotator.class);
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.BIGG_ANNOTATION_MESSAGES);


    public BiGGFBCSpeciesAnnotator(BiGGDB bigg, BiGGAnnotationParameters biGGAnnotationParameters, Registry registry) {
        super(bigg, biGGAnnotationParameters, registry);
    }

    @Override
    public void annotate(Species species) throws SQLException {
        var biGGId = findBiGGId(species);
        setFormulaFromBiGGId(species, biGGId); // Set the chemical formula and charge

        setCharge(species, biGGId);

    }

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
                            .filter(cvTerm -> cvTerm.getQualifier() == CVTerm.Qualifier.BQB_IS)
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
    private void setFormulaFromBiGGId(Species species, BiGGId biggId) throws SQLException {
        boolean isBiGGModel = species.getModel() !=null && bigg.isModel(species.getModel().getId());

        String compartmentCode = biggId.getCompartmentCode();
        var fbcSpecPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);

        boolean compartmentNonEmpty = compartmentCode != null && !compartmentCode.isEmpty();
        if (!fbcSpecPlug.isSetChemicalFormula()) {
            Optional<String> chemicalFormula = Optional.empty();
            if (isBiGGModel) {
                chemicalFormula = bigg.getChemicalFormula(biggId.getAbbreviation(), species.getModel().getId());
            }
            if ((!isBiGGModel || chemicalFormula.isEmpty()) && compartmentNonEmpty) {
                chemicalFormula = bigg.getChemicalFormulaByCompartment(biggId.getAbbreviation(), compartmentCode);
            }
            chemicalFormula.filter(SyntaxChecker::isValidChemicalFormula).ifPresent(fbcSpecPlug::setChemicalFormula);
        }
    }

    private void setCharge(Species species, BiGGId biggId) throws SQLException {
        boolean isBiGGModel = species.getModel() !=null && bigg.isModel(species.getModel().getId());
        String compartmentCode = biggId.getCompartmentCode();
        boolean compartmentNonEmpty = compartmentCode != null && !compartmentCode.isEmpty();
        var fbcSpecPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);

        Optional<Integer> chargeFromBiGG = Optional.empty();
        if (isBiGGModel) {
            chargeFromBiGG = bigg.getCharge(biggId.getAbbreviation(), species.getModel().getId());
        } else if (compartmentNonEmpty) {
            chargeFromBiGG = bigg.getChargeByCompartment(biggId.getAbbreviation(), biggId.getCompartmentCode());
        }
        if (fbcSpecPlug.isSetCharge()) {
            chargeFromBiGG
                    .filter(charge -> charge != fbcSpecPlug.getCharge())
                    .ifPresent(charge ->
                            logger.debug(format(MESSAGES.getString("CHARGE_CONTRADICTION"),
                                    charge, fbcSpecPlug.getCharge(), species.getId())));
            fbcSpecPlug.unsetCharge();
        }
        chargeFromBiGG.filter(charge -> charge != 0).ifPresent(fbcSpecPlug::setCharge);
    }

}
