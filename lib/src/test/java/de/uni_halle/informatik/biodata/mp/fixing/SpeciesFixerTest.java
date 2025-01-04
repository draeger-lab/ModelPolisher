package de.uni_halle.informatik.biodata.mp.fixing;

import de.uni_halle.informatik.biodata.mp.io.ModelReader;
import de.uni_halle.informatik.biodata.mp.io.ModelReaderException;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.polishing.ReactionsPolisherTest;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class SpeciesFixerTest {


    private final SBOParameters sboParameters = new SBOParameters();

    private SBMLDocument biomd0000000090() throws ModelReaderException {
        return new ModelReader(sboParameters, new IdentifiersOrg()).read(
                new File(ReactionsPolisherTest.class.getClassLoader().getResource("de/uni_halle/informatik/biodata/mp/models/BIOMD0000000090.xml").getFile()));
    }


    @Test
    public void biomd0000000090_does_not_fail() throws ModelReaderException {
        var model = biomd0000000090().getModel();
        assertDoesNotThrow(() -> new SpeciesFixer(List.of()).fix(model.getListOfSpecies()));
    }

}
