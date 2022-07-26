package edu.ucsd.sbrg.bigg.annotation;

import edu.ucsd.sbrg.bigg.Parameters;
import org.sbml.jsbml.SBMLDocument;

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
