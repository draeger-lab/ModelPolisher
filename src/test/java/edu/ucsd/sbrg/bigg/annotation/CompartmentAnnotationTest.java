package edu.ucsd.sbrg.bigg.annotation;

import edu.ucsd.sbrg.db.BiGGDB;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.SBO;

import static edu.ucsd.sbrg.TestUtils.assertCVTermIsPresent;
import static org.junit.jupiter.api.Assertions.*;

public class CompartmentAnnotationTest extends BiGGDBContainerTest {


    @Test
    public void annotateKnownCompartments() {
        for (var c_id : BiGGDB.getOnce("compartment")) {
            var c = new Compartment(c_id,3, 2);

            assertTrue( c.getCVTerms().isEmpty());
            assertFalse(c.isSetName());
            assertFalse(c.isSetSBOTerm());
            c.setSBOTerm(537);
            assertNotEquals(SBO.getCompartment(), c.getSBOTerm());

            var annotator = new CompartmentAnnotation(c);
            annotator.annotate();

            assertEquals(1, c.getCVTerms().size());
            assertCVTermIsPresent(c,
                    CVTerm.Type.BIOLOGICAL_QUALIFIER,
                    CVTerm.Qualifier.BQB_IS);
            assertTrue(c.isSetName());
            assertEquals(SBO.getCompartment(), c.getSBOTerm());
        }
    }

    @Test
    public void nameAnnotationIsSane() {
        var c = new Compartment("im", "default",3, 2);

        var annotator = new CompartmentAnnotation(c);
        annotator.annotate();

        assertTrue(c.isSetName());
        assertEquals("intermembrane space of mitochondria", c.getName());
    }

}

