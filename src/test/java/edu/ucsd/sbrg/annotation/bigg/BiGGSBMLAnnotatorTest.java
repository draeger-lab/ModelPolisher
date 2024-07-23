package edu.ucsd.sbrg.annotation.bigg;

import edu.ucsd.sbrg.ModelPolisherOptions;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.parameters.BiGGNotesParameters;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import static edu.ucsd.sbrg.TestUtils.assertCVTermIsPresent;
import static org.junit.jupiter.api.Assertions.*;

public class BiGGSBMLAnnotatorTest extends BiGGDBContainerTest {

    private final BiGGAnnotationParameters biGGAnnotationParameters = new BiGGAnnotationParameters(
            true,
            true,
            ModelPolisherOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue(),
            new BiGGNotesParameters(),
            null
    );
    private final SBOParameters sboParameters = new SBOParameters();

    @Test
    public void annotatePublication() {

        var sbml = new SBMLDocument(3, 2);
        var m = new Model("iJO1366", 3, 2);
        sbml.setModel(m);
        var annotator = new BiGGSBMLAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg());

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
