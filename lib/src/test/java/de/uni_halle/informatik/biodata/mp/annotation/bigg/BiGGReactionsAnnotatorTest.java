package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.annotation.adb.ADBReactionsAnnotator;
import de.uni_halle.informatik.biodata.mp.io.ModelReader;
import de.uni_halle.informatik.biodata.mp.io.ModelReaderException;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.polishing.ReactionsPolisher;
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
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.ext.groups.Member;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class BiGGReactionsAnnotatorTest extends BiGGDBContainerTest {

    private final BiGGAnnotationParameters biGGAnnotationParameters = new BiGGAnnotationParameters(
            false,
            true,
            null,
            null,
            null
    );

    private final SBOParameters sboParameters = new SBOParameters();

    public BiGGReactionsAnnotatorTest() throws ModelReaderException {
    }

    private SBMLDocument model1507180049() throws ModelReaderException {
        return new ModelReader(sboParameters, new IdentifiersOrg()).read(
                new File(ReactionsPolisherTest.class.getClassLoader().getResource("de/uni_halle/informatik/biodata/mp/models/MODEL1507180049.xml").getFile()));
    }

    private SBMLDocument model2310300002() throws ModelReaderException {
        return new ModelReader(sboParameters, new IdentifiersOrg()).read(
                new File(ReactionsPolisherTest.class.getClassLoader().getResource("de/uni_halle/informatik/biodata/mp/models/MODEL2310300002.xml").getFile()));
    }

    @Test
    public void getBiGGIdFromResourcesTest() throws SQLException {
        var m = new Model("iJO1366", 3, 2);
        var r1 = m.createReaction("some_name");
        var r2 = m.createReaction("some_other_name");
        var r3 = m.createReaction("some_third_name");

        r1.setCompartment("m");
        r1.addCVTerm(new CVTerm(
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "http://identifiers.org/biocyc/META:ACETATEKIN-RXN"));

        r2.setCompartment("e");
        r2.addCVTerm(new CVTerm(
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "http://identifiers.org/metanetx.reaction/MNXR103371"));

        r3.setCompartment("c");
        r3.addCVTerm(new CVTerm(
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "http://identifiers.org/kegg.reaction/R00299"));

        var gPlugin = (GroupsModelPlugin) m.getPlugin(GroupsConstants.shortLabel);
        assertEquals(0, gPlugin.getGroupCount());

        new BiGGReactionsAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(r1);
        new BiGGReactionsAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(r2);
        new BiGGReactionsAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(r3);

        var r1FbcPlugin = (FBCReactionPlugin) r1.getPlugin(FBCConstants.shortLabel);
        var gpa1 =  r1FbcPlugin.getGeneProductAssociation();
        assertNull(gpa1);
        assertEquals("Acetate kinase, mitochondrial", r1.getName());
        assertEquals(1, r1.getCVTermCount());
        assertEquals(11, r1.getCVTerm(0).getNumResources());

        var r2FbcPlugin = (FBCReactionPlugin) r2.getPlugin(FBCConstants.shortLabel);
        var gpa2 =  r2FbcPlugin.getGeneProductAssociation();
        assertNull(gpa2);
        assertEquals("", r2.getName());
        assertEquals(1, r2.getCVTermCount());
        assertEquals(1, r2.getCVTerm(0).getNumResources());

        var r3FbcPlugin = (FBCReactionPlugin) r3.getPlugin(FBCConstants.shortLabel);
        var gpa3 =  r3FbcPlugin.getGeneProductAssociation();
        assertNotNull(gpa3);
        assertEquals("G_b2388", ((GeneProductRef) gpa3.getAssociation()).getGeneProduct());
        assertEquals("Hexokinase (D-glucose:ATP)", r3.getName());
        assertEquals(1, r3.getCVTermCount());
        assertEquals(11, r3.getCVTerm(0).getNumResources());

        assertEquals(1, gPlugin.getGroupCount());
        assertEquals("glycolysis/gluconeogenesis", gPlugin.getGroup(0).getName());
        assertEquals(Set.of("some_third_name"), gPlugin.getGroup(0)
                .getListOfMembers().stream().map(Member::getIdRef).collect(Collectors.toSet()));

        assertFalse(r3.isSetListOfReactants());
        assertFalse(r3.isSetListOfProducts());
    }

    @Test
    public void annotationsArePulled() throws SQLException, XMLStreamException, ModelReaderException {
        var  doc = model1507180049();
        var m = doc.getModel();

        Reaction rR0009 = m.getReaction("R_r0009");
        var fbc = (FBCReactionPlugin) rR0009.getPlugin(FBCConstants.shortLabel);
        assertEquals("G_SCO1706", ((GeneProductRef) fbc.getGeneProductAssociation().getAssociation()).getGeneProduct());

        assertEquals("glyceraldehyde dehydrogenase", rR0009.getName());
        assertEquals(0, rR0009.getAnnotation().getNumCVTerms());
        new BiGGReactionsAnnotator(
                bigg,
                biGGAnnotationParameters,
                sboParameters,
                new IdentifiersOrg())
                .annotate(doc.getModel().getListOfReactions());

        assertEquals("Pyrophosphate phosphohydrolase EC:3.6.1.1", rR0009.getName());
        assertEquals(1, rR0009.getAnnotation().getNumCVTerms());
        assertEquals(58, rR0009.getCVTerm(0).getNumResources());
    }

    @Test
    public void biggAnnotationsArePulled() throws SQLException, XMLStreamException, ModelReaderException {
        var  doc = model2310300002();
        var m = doc.getModel();

        Reaction R_rxn00101_c0 = m.getReaction("R_rxn00101_c0");
        var fbc = (FBCReactionPlugin) R_rxn00101_c0.getPlugin(FBCConstants.shortLabel);
        assertEquals(1, R_rxn00101_c0.getAnnotation().getNumCVTerms());
        assertEquals(2, R_rxn00101_c0.getCVTerm(0).getNumResources());

        new ReactionsPolisher(new PolishingParameters(), sboParameters, new IdentifiersOrg())
                .polish(doc.getModel().getListOfReactions());
        new BiGGReactionsAnnotator(
                bigg,
                biGGAnnotationParameters,
                sboParameters,
                new IdentifiersOrg())
                .annotate(doc.getModel().getListOfReactions());

        assertEquals(1, R_rxn00101_c0.getAnnotation().getNumCVTerms());
        assertEquals(
                Set.of(
                        "https://identifiers.org/seed.reaction/rxn00101",
                        "https://identifiers.org/kegg.reaction/R00131",
                        "https://identifiers.org/bigg.reaction/UREA",
                        "https://identifiers.org/biocyc/META:UREASE-RXN",
                        "https://identifiers.org/ec-code/3.5.1.5",
                        "https://identifiers.org/metanetx.reaction/MNXR105153",
                        "https://identifiers.org/rhea/20557",
                        "https://identifiers.org/rhea/20558",
                        "https://identifiers.org/rhea/20559",
                        "https://identifiers.org/rhea/20560"),
                new HashSet<>(R_rxn00101_c0.getCVTerm(0).getResources()));
    }
}
