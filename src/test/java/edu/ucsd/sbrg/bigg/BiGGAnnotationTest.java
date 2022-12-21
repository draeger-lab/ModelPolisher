package edu.ucsd.sbrg.bigg;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;

import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.util.UpdateListener;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BiGGAnnotationTest {

  private static Parameters parameters;
  private static SBMLDocument doc;

//  @BeforeAll
//  public static void setUp() throws IOException, XMLStreamException {
//    // get test file
//    String input = BiGGAnnotationTest.class.getResource("models/ecoli_core.xml").getFile();
//    doc = SBMLReader.read(new File(input), new UpdateListener());
//    // DB connection
//    BiGGDB.init("biggdb", "5432", "postgres", "postgres", "bigg");
//  }
//
//
//  @Test
//  public void getBiGGIdFromResourcesTest() {
//    for (Species species : doc.getModel().getListOfSpecies()) {
//      Species obfuscated = species.clone();
//      obfuscated.setId("random");
//      BiGGAnnotation biGGAnnotation = new BiGGAnnotation();
//      String originalId = biGGAnnotation.checkId(species).map(BiGGId::toBiGGId).get();
//      String obfuscatedId = biGGAnnotation.checkId(species).map(BiGGId::toBiGGId).get();
//      assertEquals(originalId, obfuscatedId);
//    }
//  }
//
//
//  @AfterAll
//  public static void cleanUp() {
//    if (BiGGDB.inUse())
//      BiGGDB.close();
//  }
}
