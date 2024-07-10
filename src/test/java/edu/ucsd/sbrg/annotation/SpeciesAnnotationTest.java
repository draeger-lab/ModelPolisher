package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;

import java.util.Set;

import static edu.ucsd.sbrg.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpeciesAnnotationTest extends BiGGDBContainerTest {

    @BeforeEach
    public void init() {
        TestUtils.initParameters();
    }

    @Test
    public void basicAnnotationTest() {
        var m = new Model(3, 2);
        var s = m.createSpecies("atp");
        var sFbcPlugin = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);

        var annotator = new SpeciesAnnotation(s);
        annotator.annotate();

        assertEquals("atp", s.getId());
        assertEquals("ATP C10H12N5O13P3", s.getName());
        assertEquals("SBO:0000240", s.getSBOTermID());
        assertCVTermIsPresent(s,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/bigg.metabolite/atp");
        assertEquals(null, sFbcPlugin.getChemicalFormula());
    }

    @Test
    public void unknownMetaboliteCanBeInferredFromId() {
        var m = new Model(3, 2);
        var s = m.createSpecies("atp_c");

        var annotator = new SpeciesAnnotation(s);
        annotator.annotate();

        assertEquals("atp_c", s.getId());
        assertEquals("ATP C10H12N5O13P3", s.getName());
        assertEquals("SBO:0000240", s.getSBOTermID());
        assertCVTermsArePresent(s,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                expectedATPAnnotations,
                "Expected annotations are not present.");
    }

    @Test
    public void unknownMetaboliteCanBeInferredFromCV() {
        var m = new Model(3, 2);
        var s = m.createSpecies("big_chungus");

        var cvTerm = new CVTerm();
        cvTerm.setQualifier(CVTerm.Qualifier.BQB_IS);
        cvTerm.addResource("http://identifiers.org/reactome.compound/113592");
        s.addCVTerm(cvTerm);

        var annotator = new SpeciesAnnotation(s);
        annotator.annotate();

        assertEquals("big_chungus", s.getId());
        assertEquals("ATP C10H12N5O13P3", s.getName());
        assertEquals("SBO:0000240", s.getSBOTermID());
        assertEquals(1, s.getCVTermCount());
        assertEquals(30, s.getCVTerm(0).getNumResources());
        assertCVTermIsPresent(s,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "http://identifiers.org/reactome.compound/113592");
        assertCVTermsArePresent(s,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                expectedATPAnnotations,
                "Expected uris are not present.");
    }

    private static Set<String> expectedATPAnnotations = Set.of(
            "https://identifiers.org/bigg.metabolite/atp",
            "https://identifiers.org/biocyc/META:ATP",
            "https://identifiers.org/chebi/CHEBI:10789",
            "https://identifiers.org/chebi/CHEBI:10841",
            "https://identifiers.org/chebi/CHEBI:13236",
            "https://identifiers.org/chebi/CHEBI:15422",
            "https://identifiers.org/chebi/CHEBI:22249",
            "https://identifiers.org/chebi/CHEBI:2359",
            "https://identifiers.org/chebi/CHEBI:237958",
            "https://identifiers.org/chebi/CHEBI:30616",
            "https://identifiers.org/chebi/CHEBI:40938",
            "https://identifiers.org/chebi/CHEBI:57299",
            "https://identifiers.org/hmdb/HMDB00538",
            "https://identifiers.org/inchikey/ZKHQWZAMYRWXGA-KQYNXXCUSA-J",
            "https://identifiers.org/kegg.compound/C00002",
            "https://identifiers.org/kegg.drug/D08646",
            "https://identifiers.org/metanetx.chemical/MNXM3",
            "https://identifiers.org/reactome/R-ALL-113592",
            "https://identifiers.org/reactome/R-ALL-113593",
            "https://identifiers.org/reactome/R-ALL-211579",
            "https://identifiers.org/reactome/R-ALL-29358",
            "https://identifiers.org/reactome/R-ALL-389573",
            "https://identifiers.org/reactome/R-ALL-5632460",
            "https://identifiers.org/reactome/R-ALL-5696069",
            "https://identifiers.org/reactome/R-ALL-6798184",
            "https://identifiers.org/reactome/R-ALL-8869363",
            "https://identifiers.org/reactome/R-ALL-8878982",
            "https://identifiers.org/reactome/R-ALL-8938081",
            "https://identifiers.org/seed.compound/cpd00002");

    /**
     * Note this serves to document the behaviour discussed in
     * https://github.com/draeger-lab/ModelPolisher/issues/89
     *
     * The annotations as are should likely not be considered correct.
     */
    @Test
    public void H2OAnnotationTest() {
        var m = new Model(3, 2);
        var s = m.createSpecies("h2o");
        var sFbcPlugin = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);

        var annotator = new SpeciesAnnotation(s);
        annotator.annotate();

        assertEquals("h2o", s.getId());
        assertEquals("H2O H2O", s.getName());
        assertEquals("SBO:0000240", s.getSBOTermID());
        assertEquals(null, sFbcPlugin.getChemicalFormula());
        assertCVTermsArePresent(s,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                Set.of("https://identifiers.org/bigg.metabolite/h2o",
                        "https://identifiers.org/biocyc/META:CPD-15815",
                        "https://identifiers.org/biocyc/META:HYDROXYL-GROUP",
                        "https://identifiers.org/biocyc/META:OH",
                        "https://identifiers.org/biocyc/META:OXONIUM",
                        "https://identifiers.org/biocyc/META:WATER",
                        "https://identifiers.org/chebi/CHEBI:10743",
                        "https://identifiers.org/chebi/CHEBI:13352",
                        "https://identifiers.org/chebi/CHEBI:13365",
                        "https://identifiers.org/chebi/CHEBI:13419",
                        "https://identifiers.org/chebi/CHEBI:15377",
                        "https://identifiers.org/chebi/CHEBI:16234",
                        "https://identifiers.org/chebi/CHEBI:27313",
                        "https://identifiers.org/chebi/CHEBI:29356",
                        "https://identifiers.org/chebi/CHEBI:29412",
                        "https://identifiers.org/chebi/CHEBI:30490",
                        "https://identifiers.org/chebi/CHEBI:33813",
                        "https://identifiers.org/chebi/CHEBI:42043",
                        "https://identifiers.org/chebi/CHEBI:42857",
                        "https://identifiers.org/chebi/CHEBI:43228",
                        "https://identifiers.org/chebi/CHEBI:44292",
                        "https://identifiers.org/chebi/CHEBI:44641",
                        "https://identifiers.org/chebi/CHEBI:44701",
                        "https://identifiers.org/chebi/CHEBI:44819",
                        "https://identifiers.org/chebi/CHEBI:5585",
                        "https://identifiers.org/chebi/CHEBI:5594",
                        "https://identifiers.org/hmdb/HMDB01039",
                        "https://identifiers.org/hmdb/HMDB02111",
                        "https://identifiers.org/inchikey/XLYOFNOQVPJJNP-UHFFFAOYSA-N",
                        "https://identifiers.org/kegg.compound/C00001",
                        "https://identifiers.org/kegg.compound/C01328",
                        "https://identifiers.org/kegg.drug/D00001",
                        "https://identifiers.org/kegg.drug/D06322",
                        "https://identifiers.org/metanetx.chemical/MNXM2",
                        "https://identifiers.org/reactome/R-ALL-109276",
                        "https://identifiers.org/reactome/R-ALL-113518",
                        "https://identifiers.org/reactome/R-ALL-113519",
                        "https://identifiers.org/reactome/R-ALL-113521",
                        "https://identifiers.org/reactome/R-ALL-141343",
                        "https://identifiers.org/reactome/R-ALL-1605715",
                        "https://identifiers.org/reactome/R-ALL-189422",
                        "https://identifiers.org/reactome/R-ALL-2022884",
                        "https://identifiers.org/reactome/R-ALL-29356",
                        "https://identifiers.org/reactome/R-ALL-351603",
                        "https://identifiers.org/reactome/R-ALL-5278291",
                        "https://identifiers.org/reactome/R-ALL-5668574",
                        "https://identifiers.org/reactome/R-ALL-5693747",
                        "https://identifiers.org/reactome/R-ALL-8851517",
                        "https://identifiers.org/seed.compound/cpd00001",
                        "https://identifiers.org/seed.compound/cpd15275",
                        "https://identifiers.org/seed.compound/cpd27222"),
                "Expected annotations not on H2O.");

    }

    /**
     * Same principle as above, just testing for kegg and biocyc too.
     */
    @Test
    public void getBiGGIdFromResourcesTest() {
        initParameters();
        var m = new Model("iJO1366", 3, 2);
        var s1 = m.createSpecies("some_name");
        var s2 = m.createSpecies("some_other_name");

        s1.addCVTerm(new CVTerm(
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/kegg.compound/C00001"));

        s2.addCVTerm(new CVTerm(
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/biocyc/META:ATP"));


        // new SpeciesAnnotation(s1).annotate();
        new SpeciesAnnotation(s2).annotate();
//
//        assertEquals(1, s1.getCVTermCount());
//        assertEquals(29, s1.getCVTerm(0).getNumResources());
        assertEquals(1, s2.getCVTermCount());
        assertEquals(29, s2.getCVTerm(0).getNumResources());
    }

}