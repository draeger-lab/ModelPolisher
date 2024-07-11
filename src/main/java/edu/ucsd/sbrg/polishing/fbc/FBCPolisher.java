package edu.ucsd.sbrg.polishing.fbc;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.polishing.AbstractPolisher;
import edu.ucsd.sbrg.polishing.SBMLPolisher;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

public class FBCPolisher extends AbstractPolisher<Model> {

    /**
     * A {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(SBMLPolisher.class.getName());

    /**
     * Bundle for ModelPolisher logger messages
     */
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
    private final Parameters parameters;

    public FBCPolisher(Parameters parameters, List<ProgressObserver> observers) {
        super(observers);
        this.parameters = parameters;
    }

    @Override
    public void polish(Model model) {
        // Set the SBO term for the document to indicate a flux balance framework.
        model.getSBMLDocument().setSBOTerm(624);

        FBCModelPlugin modelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        // Polish the list of objectives if set
        if (modelPlug.isSetListOfObjectives()) {
            if (modelPlug.getObjectiveCount() == 0) {
                // Note: the strict attribute does not require the presence of any Objectives in the model.
                logger.warning(format(MESSAGES.getString("OBJ_MISSING"), modelPlug.getParent().getId()));
            } else {
                new FluxObjectivesPolisher(modelPlug, parameters, getObservers()).polish(modelPlug.getListOfObjectives());
            }

        }
        // Polish the list of gene products if set
        if (modelPlug.isSetListOfGeneProducts()) {
            new GeneProductsPolisher(getObservers()).polish(modelPlug.getListOfGeneProducts());
        }

        boolean strict = new StrictnessPredicate().test(model);
        // Apply strictness setting to the FBC model plugin
        modelPlug.setStrict(strict);
    }

}
