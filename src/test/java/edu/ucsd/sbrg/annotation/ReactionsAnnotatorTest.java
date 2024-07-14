package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.ModelPolisherOptions;
import edu.ucsd.sbrg.Parameters;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.GeneProductRef;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.ext.groups.Member;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.*;

public class ReactionsAnnotatorTest extends BiGGDBContainerTest {

    private final Parameters parameters = initParameters();

    @Test
    public void getBiGGIdFromResourcesTest() {
        initParameters(Map.of(ModelPolisherOptions.INCLUDE_ANY_URI.getOptionName(),
                "true"));
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

        new ReactionsAnnotator(parameters).annotate(r1);
        new ReactionsAnnotator(parameters).annotate(r2);
        new ReactionsAnnotator(parameters).annotate(r3);

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
}
