package edu.ucsd.sbrg.bigg;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class ModelPolisherTest {

  ModelPolisher polisher;


  @Before
  public void setup() {
    polisher = new ModelPolisher();
  }


  @Test
  public void testGetFileType() {
    File sbml = new File("test/resources/e_coli_core.xml");
    File mat = new File("test/resources/e_coli_core.mat");
    File json = new File("test/resources/e_coli_core.json");
    File invalid = new File("test/resources/e_coli_core.abc");
    assertTrue(Arrays.equals(new boolean[] {true, false, false},
      polisher.getFileType(sbml)));
    assertTrue(Arrays.equals(new boolean[] {false, true, false},
      polisher.getFileType(mat)));
    assertTrue(Arrays.equals(new boolean[] {false, false, true},
      polisher.getFileType(json)));
    assertTrue(Arrays.equals(new boolean[] {false, false, false},
      polisher.getFileType(invalid)));
  }
}
