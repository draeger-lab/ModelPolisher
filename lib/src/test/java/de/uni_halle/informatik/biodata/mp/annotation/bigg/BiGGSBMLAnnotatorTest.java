package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.annotation.AnnotationOptions;
import de.uni_halle.informatik.biodata.mp.annotation.AnnotationException;
import de.uni_halle.informatik.biodata.mp.io.ModelReader;
import de.uni_halle.informatik.biodata.mp.io.ModelReaderException;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGNotesParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.polishing.ReactionsPolisherTest;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.GeneProductRef;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.sql.SQLException;

import static de.uni_halle.informatik.biodata.mp.TestUtils.assertCVTermIsPresent;
import static org.junit.jupiter.api.Assertions.*;

public class BiGGSBMLAnnotatorTest extends BiGGDBContainerTest {

    private final BiGGAnnotationParameters biGGAnnotationParameters = new BiGGAnnotationParameters(
            false,
            true,
            AnnotationOptions.DOCUMENT_TITLE_PATTERN.getDefaultValue(),
            new BiGGNotesParameters(),
            null
    );

    private final SBOParameters sboParameters = new SBOParameters();


    public BiGGSBMLAnnotatorTest() throws ModelReaderException {
    }

    private SBMLDocument model1507180049() throws ModelReaderException {
        return new ModelReader(sboParameters, new IdentifiersOrg()).read(
                new File(ReactionsPolisherTest.class.getClassLoader().getResource("de/uni_halle/informatik/biodata/mp/models/MODEL1507180049.xml").getFile()));
    }

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

    @Test
    public void annotationsArePulled() throws SQLException, XMLStreamException, ModelReaderException, AnnotationException {
        var doc = model1507180049();
        var m = model1507180049().getModel();

        Reaction rR0009 = m.getReaction("R_r0009");
        var fbc = (FBCReactionPlugin) rR0009.getPlugin(FBCConstants.shortLabel);
        assertEquals("G_SCO1706", ((GeneProductRef) fbc.getGeneProductAssociation().getAssociation()).getGeneProduct());

        assertEquals("glyceraldehyde dehydrogenase", rR0009.getName());
        assertEquals(0, rR0009.getAnnotation().getNumCVTerms());
        new BiGGSBMLAnnotator(
                bigg,
                biGGAnnotationParameters,
                sboParameters,
                new IdentifiersOrg())
                .annotate(doc);

        rR0009 = doc.getModel().getReaction("R_r0009");
        assertEquals("Pyrophosphate phosphohydrolase EC:3.6.1.1", rR0009.getName());
        assertEquals(1, rR0009.getAnnotation().getNumCVTerms());
        assertEquals(58, rR0009.getCVTerm(0).getNumResources());
    }

}
