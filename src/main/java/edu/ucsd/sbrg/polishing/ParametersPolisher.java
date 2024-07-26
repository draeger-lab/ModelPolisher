package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.text.MessageFormat.format;

public class ParametersPolisher extends AbstractPolisher<Parameter> {
    private static final Logger logger = LoggerFactory.getLogger(ParametersPolisher.class);

    public ParametersPolisher(PolishingParameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(parameters, registry, observers);
    }

    /**
     * Iterates over all parameters in the model and polishes each one.
     * Displays progress for each parameter polished.
     */
    @Override
    public void polish(List<Parameter> modelParameters) {
        logger.debug("Polish Parameters");
        for (Parameter parameter : modelParameters) {
            diffReport("parameter", parameter.clone(), parameter);
            statusReport("Polishing Parameters (9/9)  ", parameter);
            polish(parameter);
        }
    }

    /**
     * Polishes the name of a parameter if it is not already set.
     * This method checks if the parameter has an ID but no name.
     * If the condition is true, it sets the parameter's name to a polished version of its ID.
     * The polishing is done using the {@link PolishingUtils#polishName(String)} method.
     *
     * @param p The parameter to be polished.
     */
    @Override
    public void polish(Parameter p) {
        if (p.isSetId() && !p.isSetName()) {
            p.setName(PolishingUtils.polishName(p.getId()));
        }
    }

}
