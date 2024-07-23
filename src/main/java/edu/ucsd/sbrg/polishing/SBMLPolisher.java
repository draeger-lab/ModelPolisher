package edu.ucsd.sbrg.polishing;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.fbc.converters.CobraToFbcV2Converter;
import org.sbml.jsbml.util.SBMLtools;
import org.sbml.jsbml.util.ValuePair;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class SBMLPolisher extends AbstractPolisher<SBMLDocument> {

    private static final Logger logger = Logger.getLogger(SBMLPolisher.class.getName());
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
    private final SBOParameters sboParameters;

    public SBMLPolisher(PolishingParameters parameters, SBOParameters sboParameters, Registry registry) {
        super(parameters, registry);
        this.sboParameters = sboParameters;
    }

    public SBMLPolisher(PolishingParameters parameters, SBOParameters sboParameters, Registry registry, List<ProgressObserver> observers) {
        super(parameters, registry, observers);
        this.sboParameters = sboParameters;
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
        if (!doc.isSetLevelAndVersion() || (doc.getLevelAndVersion().compareTo(ValuePair.of(3, 1)) < 0)) {
            logger.info(MESSAGES.getString("TRY_CONV_LVL3_V1"));
            SBMLtools.setLevelAndVersion(doc, 3, 1);
        }

        // Initialize the converter for Cobra to FBC version 2
        CobraToFbcV2Converter converter = new CobraToFbcV2Converter();
        doc = converter.convert(doc);

        // Polish the model.
        Model model = doc.getModel();
        new ModelPolisher(polishingParameters, sboParameters, registry, getObservers()).polish(model);

        // Process any external resources linked in the document's annotations.
        new AnnotationPolisher(polishingParameters, registry).polish(doc.getAnnotation());
    }

}
