package de.uni_halle.informatik.biodata.mp.annotation.bigg;

import de.uni_halle.informatik.biodata.mp.parameters.BiGGAnnotationParameters;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.SBO;

import java.sql.SQLException;

import static de.uni_halle.informatik.biodata.mp.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class BiGGCompartmentsAnnotatorTest extends BiGGDBContainerTest {

    private final BiGGAnnotationParameters parameters = new BiGGAnnotationParameters();

    @Test
    public void annotateKnownCompartments() throws SQLException {
        for (var c_id : bigg.getAllBiggIds("compartment")) {
            var c = new Compartment(c_id,3, 2);

            assertTrue( c.getCVTerms().isEmpty());
            assertFalse(c.isSetName());
            assertFalse(c.isSetSBOTerm());
            c.setSBOTerm(537);
            assertNotEquals(SBO.getCompartment(), c.getSBOTerm());

            new BiGGCompartmentsAnnotator(bigg, parameters, new IdentifiersOrg()).annotate(c);

            assertEquals(1, c.getCVTerms().size());
            assertCVTermIsPresent(c,
                    CVTerm.Type.BIOLOGICAL_QUALIFIER,
                    CVTerm.Qualifier.BQB_IS);
            assertTrue(c.isSetName());
            assertEquals(SBO.getCompartment(), c.getSBOTerm());
        }
    }

    @Test
    public void nameAnnotationIsSane() throws SQLException {
        var c = new Compartment("im", "default",3, 2);

        new BiGGCompartmentsAnnotator(bigg, parameters, new IdentifiersOrg()).annotate(c);

        assertTrue(c.isSetName());
        assertEquals("intermembrane space of mitochondria", c.getName());
    }

}

