package edu.ucsd.sbrg.bigg;

import static org.junit.Assert.assertTrue;

import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import org.jdom2.JDOMException;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

/**
 * @author Kaustubh Trivedi
 */
public class CombineArchiveTest {

    final static String modelLocation = "../../../../../resources/edu/ucsd/sbrg/bigg/e_coli_core.xml";
    final static String glossaryLocation = "../../../../../resources/edu/ucsd/sbrg/bigg/e_coli_core_glossary.rdf";
    final static String archiveLocation = "../../../../../resources/edu/ucsd/sbrg/bigg/e_coli_core.zip";

    @BeforeClass
    public static void produceCombine() throws JDOMException, CombineArchiveException, ParseException, IOException, URISyntaxException, TransformerException {
        File caFile = new File(archiveLocation);
        if(caFile.exists()){
            caFile.delete();
        }

        CombineArchive ca = new CombineArchive(caFile);

        ArchiveEntry SBMLOutput = ca.addEntry(
                new File(modelLocation),
                "model.xml",
                new URI("http://identifiers.org/combine.specifications/sbml"),
                true);

        ArchiveEntry RDFOutput = ca.addEntry(
                new File(glossaryLocation),
                "glossary.rdf",
                new URI(""),
                true);

        ca.pack();
        ca.close();
    }

    @Test
    public void testArchiveComponents() throws JDOMException, CombineArchiveException, ParseException, IOException {
        File caFile = new File(archiveLocation);
        assertTrue(caFile.exists());

        CombineArchive ca = new CombineArchive(caFile);
        boolean hasModel = false;
        boolean hasGlossary = false;

        for(ArchiveEntry archiveEntry : ca.getEntries()){
            hasModel = (archiveEntry.getFileName().equals("model.xml"));
            hasGlossary = (archiveEntry.getFileName().equals("glossary.rdf"));
        }

        assertTrue(hasModel);
        assertTrue(hasGlossary);
    }

}
