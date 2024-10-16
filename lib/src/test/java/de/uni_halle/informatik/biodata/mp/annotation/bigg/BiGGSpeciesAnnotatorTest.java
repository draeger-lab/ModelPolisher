package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.io.ModelReader;
import de.uni_halle.informatik.biodata.mp.io.ModelReaderException;
import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.polishing.ReactionsPolisherTest;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.GeneProductRef;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.sql.SQLException;
import java.util.Set;

import static de.uni_halle.informatik.biodata.mp.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BiGGSpeciesAnnotatorTest extends BiGGDBContainerTest {

    private final BiGGAnnotationParameters biGGAnnotationParameters = new BiGGAnnotationParameters();
    private final SBOParameters sboParameters = new SBOParameters();


    private SBMLDocument modelGCF_000021565() throws ModelReaderException {
        return new ModelReader(sboParameters, new IdentifiersOrg()).read(
                new File(ReactionsPolisherTest.class.getClassLoader().getResource("de/uni_halle/informatik/biodata/mp/models/GCF_000021565.1.xml").getFile()));
    }


    @Test
    public void basicAnnotationTest() throws SQLException {
        var m = new Model(3, 2);
        var s = m.createSpecies("atp");
        var sFbcPlugin = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);

        new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(s);

        assertEquals("atp", s.getId());
        assertEquals("ATP C10H12N5O13P3", s.getName());
        assertCVTermIsPresent(s,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/bigg.metabolite/atp");
        assertNull(sFbcPlugin.getChemicalFormula());
    }

    @Test
    public void unknownMetaboliteCanBeInferredFromId() throws SQLException {
        var m = new Model(3, 2);
        var s = m.createSpecies("atp_c");

        new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(s);

        assertEquals("atp_c", s.getId());
        assertEquals("ATP C10H12N5O13P3", s.getName());
        assertCVTermsArePresent(s,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                expectedATPAnnotations,
                "Expected annotations are not present.");
    }

    @Test
    public void unknownMetaboliteCanBeInferredFromCV() throws SQLException {
        var m = new Model(3, 2);
        var s = m.createSpecies("big_chungus");

        var cvTerm = new CVTerm();
        cvTerm.setQualifier(CVTerm.Qualifier.BQB_IS);
        cvTerm.addResource("http://identifiers.org/reactome.compound/113592");
        s.addCVTerm(cvTerm);

        new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(s);

        assertEquals("big_chungus", s.getId());
        assertEquals("ATP C10H12N5O13P3", s.getName());
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

    private static final Set<String> expectedATPAnnotations = Set.of(
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
     * <a href="https://github.com/draeger-lab/ModelPolisher/issues/89">...</a>
     * <p>
     * The annotations as are should likely not be considered correct.
     */
    @Test
    public void H2OAnnotationTest() throws SQLException {
        var m = new Model(3, 2);
        var s = m.createSpecies("h2o");
        var sFbcPlugin = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);

        new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(s);

        assertEquals("h2o", s.getId());
        assertEquals("H2O H2O", s.getName());
        assertNull(sFbcPlugin.getChemicalFormula());
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
    public void getBiGGIdFromResourcesTest() throws SQLException {
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
        new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(s2);
//
//        assertEquals(1, s1.getCVTermCount());
//        assertEquals(29, s1.getCVTerm(0).getNumResources());
        assertEquals(1, s2.getCVTermCount());
        assertEquals(29, s2.getCVTerm(0).getNumResources());
    }

    @Test
    public void duplicatePatterns() throws SQLException {
        var m = new Model(3, 2);
        var s1 = m.createSpecies("big_chungus1");
        var s2 = m.createSpecies("big_chungus2");

        var cvTerm1 = new CVTerm();
        cvTerm1.setQualifier(CVTerm.Qualifier.BQB_IS);
        cvTerm1.addResource("https://identifiers.org/bigg.metabolite:10fthf");
        s1.addCVTerm(cvTerm1);

        var cvTerm2 = new CVTerm();
        cvTerm2.setQualifier(CVTerm.Qualifier.BQB_IS);
        cvTerm2.addResource("https://identifiers.org/bigg.metabolite/10fthf");
        s2.addCVTerm(cvTerm2);

        new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(s1);
        new BiGGSpeciesAnnotator(bigg, biGGAnnotationParameters, sboParameters, new IdentifiersOrg()).annotate(s2);

        assertEquals("big_chungus1", s1.getId());
        assertEquals("10-Formyltetrahydrofolate", s1.getName());
        assertEquals(1, s1.getCVTermCount());
        assertEquals(15, s1.getCVTerm(0).getNumResources());
        assertCVTermIsPresent(s1,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/bigg.metabolite:10fthf");
        assertCVTermIsPresent(s1,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/reactome/R-ALL-419151");
        assertEquals(1,
                s1.getCVTerm(0).getResources()
                        .stream()
                        .filter(resource -> resource.contains("bigg.metabolite"))
                        .count());

        assertEquals("big_chungus2", s2.getId());
        assertEquals("10-Formyltetrahydrofolate", s2.getName());
        assertEquals(1, s2.getCVTermCount());
        assertEquals(15, s2.getCVTerm(0).getNumResources());
        assertCVTermIsPresent(s2,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/bigg.metabolite/10fthf");
        assertCVTermIsPresent(s2,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS,
                "https://identifiers.org/reactome/R-ALL-419151");
        assertEquals(1,
                s2.getCVTerm(0).getResources()
                        .stream()
                        .filter(resource -> resource.contains("bigg.metabolite"))
                        .count());
    }

    @Test
    public void annotationsArePulled() throws SQLException, ModelReaderException {
        var doc = modelGCF_000021565();
        var m = doc.getModel();

        Species M_10fthf_c = m.getSpecies("M_10fthf_c");

        assertEquals("10-Formyltetrahydrofolate", M_10fthf_c.getName());
        assertEquals(1, M_10fthf_c.getAnnotation().getNumCVTerms());
        assertEquals(14, M_10fthf_c.getCVTerm(0).getNumResources());
        new BiGGSpeciesAnnotator(
                bigg,
                biGGAnnotationParameters,
                sboParameters,
                new IdentifiersOrg())
                .annotate(doc.getModel().getListOfSpecies());

        assertEquals(1, M_10fthf_c.getAnnotation().getNumCVTerms());
        assertEquals(15, M_10fthf_c.getCVTerm(0).getNumResources());
    }

}