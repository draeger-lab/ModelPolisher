package edu.ucsd.sbrg.bigg.annotation;

import edu.ucsd.sbrg.ModelPolisherOptions;
import edu.ucsd.sbrg.annotation.ModelAnnotation;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;

import java.util.Map;

import static edu.ucsd.sbrg.TestUtils.assertCVTermIsPresent;
import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.*;

public class ModelAnnotationTest extends BiGGDBContainerTest {

    @Test
    public void basicModelAnnotation() {
        initParameters(Map.of(
                ModelPolisherOptions.INCLUDE_ANY_URI.getOptionName(),
                "true"));
        var m = new Model("iJO1366", 3,2);
        var annotater = new ModelAnnotation(m);

        assertFalse(m.isSetMetaId());
        assertTrue(m.getCVTerms().isEmpty());

        annotater.annotate();

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
                "https://identifiers.org/refseq:NC_000913.3",
                "Expected NCBI refseq accession NC_000913.3 not present on the model.");
    }

}
