package de.uni_halle.informatik.biodata.mp.fixing;

import de.uni_halle.informatik.biodata.mp.parameters.FixingParameters;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import java.util.List;

public class SBMLFixer extends AbstractFixer implements IFixSBases<SBMLDocument> {

    private final FixingParameters fixingParameters;

    public SBMLFixer(FixingParameters fixingParameters) {
        super();
        this.fixingParameters = fixingParameters;
    }

    public SBMLFixer(FixingParameters fixingParameters, List<ProgressObserver> observers) {
        super(observers);
        this.fixingParameters = fixingParameters;
    }

    @Override
    public void fix(SBMLDocument sbmlDocument, int index) {
        fixMissingModel(sbmlDocument);
        new ModelFixer(fixingParameters, getObservers()).fix(sbmlDocument.getModel(), 0);
    }

    private void fixMissingModel(SBMLDocument sbmlDocument) {
        if (!sbmlDocument.isSetModel()) {
            sbmlDocument.setModel(new Model());
        }
    }
}
