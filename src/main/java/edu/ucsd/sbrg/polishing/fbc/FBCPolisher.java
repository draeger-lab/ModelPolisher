package edu.ucsd.sbrg.polishing.fbc;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.polishing.AbstractPolisher;
import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.polishing.IPolishSBases;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

import static java.text.MessageFormat.format;

public class FBCPolisher extends AbstractPolisher implements IPolishSBases<Model> {

    private static final Logger logger = LoggerFactory.getLogger(FBCPolisher.class);
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
    private final SBOParameters sboParameters;

    public FBCPolisher(PolishingParameters parameters, SBOParameters sboParameters, Registry registry, List<ProgressObserver> observers) {
        super(parameters, registry, observers);
        this.sboParameters = sboParameters;
    }

    @Override
    public void polish(Model model) {
        logger.debug("Polish FBC Plugin");
        // Set the SBO term for the document to indicate a flux balance framework.
        model.getSBMLDocument().setSBOTerm(624);

        FBCModelPlugin modelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        // Polish the list of objectives if set
        if (modelPlug.isSetListOfObjectives()) {
            if (modelPlug.getObjectiveCount() == 0) {
                logger.info(format(MESSAGES.getString("OBJ_MISSING"), modelPlug.getParent().getId()));
            } else {
                new ObjectivesPolisher(modelPlug, polishingParameters, registry, getObservers())
                        .polish(modelPlug.getListOfObjectives());
                new FBCReactionPolisher(modelPlug, polishingParameters, sboParameters, registry, getObservers())
                        .polish(model.getListOfReactions());
            }

        }
        // Polish the list of gene products if set
        if (modelPlug.isSetListOfGeneProducts()) {
            new GeneProductsPolisher(polishingParameters, registry, getObservers()).polish(modelPlug.getListOfGeneProducts());
        }

        boolean strict = new StrictnessPredicate().test(model);
        // Apply strictness setting to the FBC model plugin
        modelPlug.setStrict(strict);
    }

}
