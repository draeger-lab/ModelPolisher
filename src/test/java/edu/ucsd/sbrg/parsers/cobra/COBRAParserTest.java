package edu.ucsd.sbrg.parsers.cobra;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.util.ModelBuilder;

import java.io.File;
import java.io.IOException;

import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class COBRAParserTest {

    private static ModelBuilder builder;

    /**
     * This is here to call the static initializer code of the Registry class,
     * which in particular serves to initialize the edu.ucsd.sbrg.miriam.Entries
     * singleton.
     */
    @BeforeAll
    public static void setUp() throws ClassNotFoundException {
        Class.forName("edu.ucsd.sbrg.miriam.Registry");
    }

    @Test
    public void parsingDoesNotThrowErrors() {
        initParameters();
        var recon = new File(COBRAParserTest.class.getResource("Recon3D.mat").getFile());
        try {
            var sbmlDoc = COBRAParser.read(recon);
            assertTrue(true);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false, "Parsing Recon3D.mat threw an exception.");
        }
    }

}