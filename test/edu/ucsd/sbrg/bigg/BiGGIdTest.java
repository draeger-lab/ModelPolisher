/**
 * 
 */
package edu.ucsd.sbrg.bigg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Thomas Zajac
 */
public class BiGGIdTest {

  static final String[] ID_STRINGS = {"G_1818", "R_EX_h2o_e", "M_nadh_c",
    "M_14glucan_e", "R_2AGPE181tipp", "G_SDY_0121", "M_13_cis_retnglc_c",
    "R_24_25DHVITD2tm", "G_10090_AT1", "M_20ahchsterol_m", "R_1a_24_25VITD2Hm",
    "R_3DSPHR", "G_Acmsd", "M_12dgr_HP_c", "R_BIOMASS_HP_published",
    "M_26dap__M_c", "R_BIOMASS_Ecoli_TM", "G_S_0001", "M_26dap_LL_c",
    "R_BIOMASS_Ecoli_core_w_GAM", "G_test_mm_MM"};
  static BiGGId[] testIds;


  /**
   * Initializes BiGGId-Array for testing
   */
  @BeforeClass
  public static final void setUp() {
    testIds = new BiGGId[ID_STRINGS.length];
    for (int counter = 0; counter < ID_STRINGS.length; counter++) {
      testIds[counter] = new BiGGId(ID_STRINGS[counter]);
    }
  }


  /**
   * Test method for {@link edu.ucsd.sbrg.bigg.BiGGId#hashCode()}.
   */
  @Test
  public final void testHashCode() {
    assertEquals(923521, new BiGGId().hashCode());
    assertEquals(-2105880884, testIds[0].hashCode());
  }


  /**
   * Test method for
   * {@link edu.ucsd.sbrg.bigg.BiGGId#equals(java.lang.Object)}.
   * Tests equality of both Constructors and the equals Method
   */
  @Test
  public final void testEqualsObject() {
    for (int idPos = 0; idPos < testIds.length; idPos++) {
      BiGGId firstConstructorId = new BiGGId(ID_STRINGS[idPos]);
      BiGGId testId = testIds[idPos];
      BiGGId secondConstructorId =
        new BiGGId(testId.getPrefix(), testId.getAbbreviation(),
          testId.getCompartmentCode(), testId.getTissueCode());
      assertEquals(testId, firstConstructorId);
      assertTrue(testId.equals(firstConstructorId));
      assertEquals(testId, secondConstructorId);
      assertTrue(testId.equals(secondConstructorId));
    }
  }


  /**
   * Test method for {@link edu.ucsd.sbrg.bigg.BiGGId#isSetAbbreviation()}.
   */
  @Test
  public final void testIsSetAbbreviation() {
    for (int idPos = 0; idPos < testIds.length; idPos++) {
      assertTrue(testIds[idPos].isSetAbbreviation());
    }
  }


  /**
   * Test method for {@link edu.ucsd.sbrg.bigg.BiGGId#isSetCompartmentCode()}.
   */
  @Test
  public final void testIsSetCompartmentCode() {
    assertFalse(new BiGGId().isSetCompartmentCode());
    assertFalse(testIds[0].isSetCompartmentCode());
    assertTrue(testIds[18].isSetCompartmentCode());
  }


  /**
   * Test method for {@link edu.ucsd.sbrg.bigg.BiGGId#isSetPrefix()}.
   */
  @Test
  public final void testIsSetPrefix() {
    for (int idPos = 0; idPos < testIds.length; idPos++) {
      assertTrue(testIds[idPos].isSetPrefix());
    }
    assertFalse(new BiGGId().isSetPrefix());
  }


  /**
   * Test method for {@link edu.ucsd.sbrg.bigg.BiGGId#isSetTissueCode()}.
   */
  @Test
  public final void testIsSetTissueCode() {
    assertFalse(new BiGGId().isSetTissueCode());
    assertFalse(testIds[0].isSetTissueCode());
    assertFalse(testIds[18].isSetTissueCode());
  }


  /**
   * Test method for
   * {@link edu.ucsd.sbrg.bigg.BiGGId#setCheckAbbreviation(java.lang.String)}.
   */
  @Test
  public final void testSetAbbreviation() {
    BiGGId oneTimeID = new BiGGId();
    oneTimeID.setCheckAbbreviation("");
    assertEquals("", oneTimeID.getAbbreviation());
    assertEquals("26dap_LL", testIds[18].getAbbreviation());
  }


  /**
   * Test method for
   * {@link edu.ucsd.sbrg.bigg.BiGGId#setCheckCompartmentCode(java.lang.String)}
   * .
   */
  @Test
  public final void testSetCheckCompartmentCode() {
    assertEquals("c", testIds[18].getCompartmentCode());
    assertEquals("mm", testIds[20].getCompartmentCode());
  }


  /**
   * Test method for
   * {@link edu.ucsd.sbrg.bigg.BiGGId#setParsedPrefix(java.lang.String)}.
   */
  @Test
  public final void testSetParsedPrefix() {
    BiGGId oneTimeId = new BiGGId();
    oneTimeId.setParsedPrefix("R_EX_FLUENT");
    assertEquals("R_EX", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setParsedPrefix("R_DM_MORE_ENERGY");
    assertEquals("R_DM", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setParsedPrefix("R_BIOMASS_MASS");
    assertEquals("R_BIOMASS", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setParsedPrefix("R_I_AM_A_REACTION");
    assertEquals("R", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setParsedPrefix("G_IACZ");
    assertEquals("G", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setParsedPrefix("L_NOT_AN_ID");
    assertEquals("", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setParsedPrefix("");
    assertEquals("", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
  }


  /**
   * Test method for
   * {@link edu.ucsd.sbrg.bigg.BiGGId#setConstructorPrefix(java.lang.String)}.
   */
  @Test
  public final void testSetConstructorPrefix() {
    BiGGId oneTimeId = new BiGGId();
    oneTimeId.setConstructorPrefix("R_EX_FLUENT");
    assertEquals("", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setConstructorPrefix("R_EX");
    assertEquals("R_EX", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setConstructorPrefix("R_DM");
    assertEquals("R_DM", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setConstructorPrefix("R_BIOMASS");
    assertEquals("R_BIOMASS", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setConstructorPrefix("R");
    assertEquals("R", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setConstructorPrefix("G");
    assertEquals("G", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setConstructorPrefix("L_NOT_AN_ID");
    assertEquals("", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
    oneTimeId.setConstructorPrefix("");
    assertEquals("", oneTimeId.getPrefix());
    oneTimeId.unsetPrefix();
  }


  /**
   * Test method for
   * {@link edu.ucsd.sbrg.bigg.BiGGId#setCheckTissueCode(java.lang.String)}.
   */
  @Test
  public final void testSetCheckTissueCode() {
    assertEquals("", testIds[18].getTissueCode());
    assertEquals("MM", testIds[20].getTissueCode());
  }


  /**
   * Test method for {@link edu.ucsd.sbrg.bigg.BiGGId#toBiGGId()}.
   */
  @Test
  public final void testToBiGGId() {
    for (int idPos = 0; idPos < testIds.length; idPos++) {
      assertEquals(ID_STRINGS[idPos], testIds[idPos].toBiGGId());
    }
  }


  @AfterClass
  public static final void cleanUp() {
    testIds = null;
  }
}
