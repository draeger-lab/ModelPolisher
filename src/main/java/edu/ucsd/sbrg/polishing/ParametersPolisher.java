package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.resolver.Registry;
import org.sbml.jsbml.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ParametersPolisher extends AbstractPolisher implements IPolishSBases<Parameter> {
    private static final Logger logger = LoggerFactory.getLogger(ParametersPolisher.class);

    public ParametersPolisher(PolishingParameters parameters, Registry registry, List<ProgressObserver> observers) {
        super(parameters, registry, observers);
    }

    @Override
    public void polish(List<Parameter> modelParameters) {
        logger.debug("Polish Parameters");
        for (Parameter parameter : modelParameters) {
            statusReport("Polishing Parameters (9/9)  ", parameter);
            polish(parameter);
        }
    }

    @Override
    public void polish(Parameter p) {
        if (!p.isSetName()) {
            p.setName(p.getId());
            new NamePolisher().polish(p);
        }
    }

}
