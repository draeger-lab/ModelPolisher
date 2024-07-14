package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.ModelPolisherOptions;
import edu.ucsd.sbrg.Parameters;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import java.util.Map;

import static edu.ucsd.sbrg.TestUtils.assertCVTermIsPresent;
import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.*;

public class BiGGAnnotatorTest extends BiGGDBContainerTest {

    private final Parameters parameters = initParameters();

    @Test
    public void annotatePublication() {
        initParameters(Map.of(
                ModelPolisherOptions.ANNOTATE_WITH_BIGG.getOptionName(),
                "true",
                ModelPolisherOptions.INCLUDE_ANY_URI.getOptionName(),
                "true"));
        var sbml = new SBMLDocument(3, 2);
        var m = new Model("iJO1366", 3, 2);
        sbml.setModel(m);
        var annotator = new BiGGAnnotator(parameters);

        assertFalse(m.isSetMetaId());
        assertTrue(m.getCVTerms().isEmpty());

        annotator.annotate(sbml);

        assertTrue(m.isSetMetaId());
        assertCVTermIsPresent(m,
                CVTerm.Type.MODEL_QUALIFIER,
                CVTerm.Qualifier.BQM_IS_DESCRIBED_BY,
                "https://identifiers.org/pubmed/21988831",
                "Expected pubmed publication reference 21988831 not present on the model.");
    }

}
