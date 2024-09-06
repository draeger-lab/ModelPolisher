package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.annotation.AnnotationOptions;
import de.uni_halle.informatik.biodata.mp.annotation.AnnotationException;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGNotesParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import java.sql.SQLException;

import static de.uni_halle.informatik.biodata.mp.TestUtils.assertCVTermIsPresent;
import static org.junit.jupiter.api.Assertions.*;

public class BiGGSBMLAnnotatorTest extends BiGGDBContainerTest {

    private final BiGGAnnotationParameters biGGAnnotationParameters = new BiGGAnnotationParameters(
            true,
            true,
            AnnotationOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue(),
            new BiGGNotesParameters(),
            null
    );
    private final SBOParameters sboParameters = new SBOParameters();

    @Test
    public void annotatePublication() throws SQLException, AnnotationException {

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
