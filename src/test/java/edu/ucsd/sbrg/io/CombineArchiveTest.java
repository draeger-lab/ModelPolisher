package edu.ucsd.sbrg.io;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

import static org.junit.Assert.assertTrue;

/**
 * @author Kaustubh Trivedi
 */
public class CombineArchiveTest {

    @BeforeAll
    public static void produceCombine() throws JDOMException, CombineArchiveException, ParseException, IOException, URISyntaxException, TransformerException {
        File modelFile = new File(CombineArchiveTest.class.getResource("model.xml").getFile());
        File glossaryFile = new File(CombineArchiveTest.class.getResource("glossary.rdf").getFile());

        CombineArchive ca = new CombineArchive(new File(modelFile.getAbsolutePath().substring(0, modelFile.getAbsolutePath().lastIndexOf('.')) + ".zip"));

        ca.addEntry(modelFile,
                "model.xml",
                new URI("http://identifiers.org/combine.specifications/sbml"),
                true);

        ca.addEntry(glossaryFile,
                "glossary.rdf",
                //generated from https://sems.uni-rostock.de/trac/combine-ext/wiki/CombineFormatizer
                new URI("http://purl.org/NET/mediatypes/application/rdf+xml"),
                true);

        ca.pack();
        ca.close();
    }

    @Test
    public void testArchiveComponents() throws JDOMException, CombineArchiveException, ParseException, IOException {
        String modelLocation = CombineArchiveTest.class.getResource("model.xml").getFile();
        File caFile = new File(modelLocation.substring(0, modelLocation.lastIndexOf('.')) + ".zip");
        assertTrue(caFile.exists());

        try (CombineArchive ca = new CombineArchive(caFile, true)) {
            boolean hasModel = false;
            boolean hasGlossary = false;

            for (ArchiveEntry archiveEntry : ca.getEntries()) {
                if (!hasModel) {
                    hasModel = archiveEntry.getFileName().equals("model.xml");
                }
                if (!hasGlossary) {
                    hasGlossary = archiveEntry.getFileName().equals("glossary.rdf");
                }
            }

            assertTrue(hasModel);
            assertTrue(hasGlossary);
        }
    }

    @AfterAll
    public static void cleanUp(){
        String modelLocation = CombineArchiveTest.class.getResource("model.xml").getFile();
        File caFile = new File(modelLocation.substring(0, modelLocation.lastIndexOf('.')) + ".zip");
        if(caFile.exists()) {
            caFile.delete();
        }
    }
}
