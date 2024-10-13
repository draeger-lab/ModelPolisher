package de.uni_halle.informatik.biodata.mp.annotation.bigg.ext.fbc;

import de.uni_halle.informatik.biodata.mp.annotation.bigg.BiGGDBContainerTest;
import de.uni_halle.informatik.biodata.mp.annotation.bigg.BiGGReactionsAnnotator;
import de.uni_halle.informatik.biodata.mp.io.ModelReader;
import de.uni_halle.informatik.biodata.mp.io.ModelReaderException;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.polishing.ReactionsPolisherTest;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.GeneProductRef;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BiGGGeneProductAnnotatorTest extends BiGGDBContainerTest {


    private final BiGGAnnotationParameters biGGAnnotationParameters = new BiGGAnnotationParameters(
            false,
            true,
            null,
            null,
            null
    );


    private final SBOParameters sboParameters = new SBOParameters();

    private SBMLDocument model1507180060() throws ModelReaderException {
        return new ModelReader(sboParameters, new IdentifiersOrg()).read(
                new File(ReactionsPolisherTest.class.getClassLoader().getResource("de/uni_halle/informatik/biodata/mp/models/MODEL1507180060.xml").getFile()));
    }

    @Test
    public void annotationsArePulled() throws SQLException, XMLStreamException, ModelReaderException {
        var  doc = model1507180060();
        var m = doc.getModel();

        FBCModelPlugin fbc = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
        var G_b0002 = fbc.getGeneProduct("G_b0002");

        assertEquals(0, G_b0002.getAnnotation().getNumCVTerms());
        new BiGGGeneProductAnnotator(
                new BiGGGeneProductReferencesAnnotator(),
                bigg,
                biGGAnnotationParameters,
                new IdentifiersOrg(),
                new ArrayList<>())
                .annotate(fbc.getListOfGeneProducts());

        assertEquals(2, G_b0002.getAnnotation().getNumCVTerms());
        assertEquals(1, G_b0002.getCVTerm(0).getNumResources());
        assertEquals(G_b0002.getCVTerm(0).getResources(),
                List.of(
                        "https://identifiers.org/uniprot/P00561"
                ));
        assertEquals(G_b0002.getCVTerm(1).getResources(),
                List.of(
                        "https://identifiers.org/asap/ABE-0000008",
                        "https://identifiers.org/ecogene/EG10998",
                        "https://identifiers.org/ncbigene/945803"
                ));
    }
}
