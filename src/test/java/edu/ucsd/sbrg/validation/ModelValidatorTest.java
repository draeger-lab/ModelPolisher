package edu.ucsd.sbrg.validation;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.validator.offline.LoggingValidationContext;

import static org.junit.jupiter.api.Assertions.*;

public class ModelValidatorTest {

    @Test
    public void invalidSBMLFails() {
        var doc = new SBMLDocument(3, 1);
        doc.setModel(new Model());
//        var validator = new ModelValidator();
//        var errorLog = validator.validate(sbml);
        LoggingValidationContext context = new LoggingValidationContext(doc.getLevel(), doc.getVersion());
        context.setValidateRecursively(true);
        context.loadConstraints(Model.class, doc.getLevel(), doc.getVersion());
        context.validate(doc.getModel());
        var errorLog = context.getErrorLog();
        assertEquals(0, errorLog.getErrorCount());
        assertFalse(doc.getModel().isSetId());
    }
}
