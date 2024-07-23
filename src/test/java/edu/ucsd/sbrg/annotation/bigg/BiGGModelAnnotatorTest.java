package edu.ucsd.sbrg.annotation.bigg;

import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;

import static edu.ucsd.sbrg.TestUtils.assertCVTermIsPresent;
import static org.junit.jupiter.api.Assertions.*;

public class BiGGModelAnnotatorTest extends BiGGDBContainerTest {

    private final BiGGAnnotationParameters biGGAnnotationParameters = new BiGGAnnotationParameters(
            false,
            true,
            null,
            null,
            null
    );

    @Test
    public void basicModelAnnotation() {
        var m = new Model("iJO1366", 3,2);

        assertFalse(m.isSetMetaId());
        assertTrue(m.getCVTerms().isEmpty());

        new BiGGModelAnnotator(bigg, biGGAnnotationParameters, new IdentifiersOrg()).annotate(m);

        assertTrue(m.isSetMetaId());
        assertEquals(3, m.getCVTerms().size());
        assertCVTermIsPresent(m,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_HAS_TAXON,
                "https://identifiers.org/taxonomy/511145");
        assertCVTermIsPresent(m,
                CVTerm.Type.MODEL_QUALIFIER,
                CVTerm.Qualifier.BQM_IS,
                "https://identifiers.org/bigg.model/iJO1366");
        assertCVTermIsPresent(m,
                CVTerm.Type.BIOLOGICAL_QUALIFIER,
                CVTerm.Qualifier.BQB_IS_VERSION_OF,
                "https://identifiers.org/refseq/NC_000913.3",
                "Expected NCBI refseq accession NC_000913.3 not present on the model.");
    }

}
