package edu.ucsd.sbrg.fixing;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

public class SBMLFixer implements IFixSBases<SBMLDocument> {

    @Override
    public void fix(SBMLDocument sbmlDocument, int index) {
        fixMissingModel(sbmlDocument);
        new ModelFixer().fix(sbmlDocument.getModel(), 0);
    }

    private void fixMissingModel(SBMLDocument sbmlDocument) {
        if (!sbmlDocument.isSetModel()) {
            sbmlDocument.setModel(new Model());
        }
    }
}
