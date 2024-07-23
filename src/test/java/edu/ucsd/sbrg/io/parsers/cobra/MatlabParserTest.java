package edu.ucsd.sbrg.io.parsers.cobra;

import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class MatlabParserTest {

    /**
     * This is here to call the static initializer code of the Registry class,
     * which in particular serves to initialize the edu.ucsd.sbrg.miriam.Entries
     * singleton.
     */
    @BeforeAll
    public static void setUp() throws ClassNotFoundException {
        Class.forName("edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg");
    }

    @Test
    public void parsingDoesNotThrowErrors() {
        var parameters = new SBOParameters();
        var recon = new File(MatlabParserTest.class.getResource("Recon3D.mat").getFile());
        try {
            new MatlabParser(parameters, new IdentifiersOrg()).parse(recon);
            assertTrue(true);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Parsing Recon3D.mat threw an exception.");
        }
    }

}
