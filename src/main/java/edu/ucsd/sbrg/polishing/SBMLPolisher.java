package edu.ucsd.sbrg.polishing;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.fbc.converters.CobraToFbcV2Converter;
import org.sbml.jsbml.util.SBMLtools;
import org.sbml.jsbml.util.ValuePair;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class SBMLPolisher extends AbstractPolisher<SBMLDocument> {

    /**
     * A {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(SBMLPolisher.class.getName());

    /**
     * Bundle for ModelPolisher logger messages
     */
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    private final Parameters parameters;

    public SBMLPolisher(Parameters parameters, List<ProgressObserver> observers) {
        super(observers);
        this.parameters = parameters;
    }

    /**
     * This method serves as the entry point from the ModelPolisher class to polish an SBML document.
     * It ensures the document contains a model, performs a sanity check, polishes the model, sets the SBO term,
     * marks the progress as finished if applicable, and processes any linked resources.
     *
     * @param doc The SBMLDocument containing the model to be polished.
     */
    @Override
    public void polish(SBMLDocument doc) {
        // Ensure the document is at the correct SBML level and version
        if (!doc.isSetLevelAndVersion() || (doc.getLevelAndVersion().compareTo(ValuePair.of(3, 2)) < 0)) {
            logger.info(MESSAGES.getString("TRY_CONV_LVL3_V2"));
            SBMLtools.setLevelAndVersion(doc, 3, 2);
        }

        // Initialize the converter for Cobra to FBC version 2
        CobraToFbcV2Converter converter = new CobraToFbcV2Converter();
        doc = converter.convert(doc);

        // Polish the model.
        Model model = doc.getModel();
        new ModelPolisher(parameters, getObservers()).polish(model);

        // Process any external resources linked in the document's annotations.
        new AnnotationPolisher().polish(doc.getAnnotation());
    }

}
