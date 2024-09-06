package de.uni_halle.informatik.biodata.mp.io.parsers.cobra;

import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MatlabParserTest {

    /**
     * This is here to call the static initializer code of the Registry class,
     * which in particular serves to initialize the de.uni_halle.informatik.biodata.mp.miriam.Entries
     * singleton.
     */
    @BeforeAll
    public static void setUp() throws ClassNotFoundException {
        Class.forName("de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg");
    }

    @Test
    public void parsingDoesNotThrowErrors() {
        var parameters = new SBOParameters();
        var recon = new File(MatlabParserTest.class.getResource("Recon3D.mat").getFile());
        try {
            new MatlabParser(parameters, new IdentifiersOrg()).parse(recon);
            assertTrue(true);
        } catch (IOException e) {
            fail("Parsing Recon3D.mat threw an exception.", e);
        }
    }

}
