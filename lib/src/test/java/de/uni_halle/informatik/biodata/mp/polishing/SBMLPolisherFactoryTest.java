package de.uni_halle.informatik.biodata.mp.polishing;

import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;

import static org.junit.jupiter.api.Assertions.*;

class SBMLPolisherFactoryTest {

    @Test
    public void compatibleVersion() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> SBMLPolisherFactory.createSBMLPolisher(new SBMLDocument(2, 1), null, null, null));

        assertThrowsExactly(IllegalArgumentException.class,
                () -> SBMLPolisherFactory.createSBMLPolisher(new SBMLDocument(2, 2), null, null, null));

        assertThrowsExactly(IllegalArgumentException.class,
                () -> SBMLPolisherFactory.createSBMLPolisher(new SBMLDocument(2, 3), null, null, null));

        assertThrowsExactly(IllegalArgumentException.class,
                () -> SBMLPolisherFactory.createSBMLPolisher(new SBMLDocument(2, 4), null, null, null));
        assertThrowsExactly(IllegalArgumentException.class,

                () -> SBMLPolisherFactory.createSBMLPolisher(new SBMLDocument(2, 5), null, null, null));

        assertThrowsExactly(IllegalArgumentException.class,
                () -> SBMLPolisherFactory.createSBMLPolisher(new SBMLDocument(3, 2), null, null, null));

        assertNotNull(SBMLPolisherFactory.createSBMLPolisher(new SBMLDocument(3, 1), new PolishingParameters(), new SBOParameters(), new IdentifiersOrg()));

    }
}