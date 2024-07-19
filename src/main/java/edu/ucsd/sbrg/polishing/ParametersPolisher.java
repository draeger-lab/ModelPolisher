package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import edu.ucsd.sbrg.reporting.ReportType;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Parameter;

import java.util.List;

public class ParametersPolisher extends AbstractPolisher<Parameter> {

    public ParametersPolisher(Parameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(parameters, registry, observers);
    }

    /**
     * Iterates over all parameters in the model and polishes each one.
     * Displays progress for each parameter polished.
     */
    @Override
    public void polish(List<Parameter> modelParameters) {
        for (Parameter parameter : modelParameters) {
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
