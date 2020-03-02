package edu.ucsd.sbrg.bigg;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * @author Andreas Dr&auml;ger
 *
 */
public class IDtest {

  @BeforeAll
  public static void setUp() {
  }


  @Test
  public void test() {
    String r1 = "R_SK_5moxac";
    String r2 = "R_sink_test";
    String r3 = "R_sdnk_test";
    String pattern = ".*_[Ss]([Ii][Nn])?[Kk]_.*";
    if (r1.matches(pattern) && r2.matches(pattern) && !r3.matches(pattern)) {
      System.out.println("pass");
    } else {
      fail();
    }
  }
}
