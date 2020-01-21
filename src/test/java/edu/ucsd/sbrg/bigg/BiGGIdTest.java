package edu.ucsd.sbrg.bigg;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Zajac
 */
public class BiGGIdTest {

  private static final List<String> ID_STRINGS = Arrays.asList("G_1818", "EX_h2o_e", "M_nadh_c", "M_14glucan_e",
      "R_2AGPE181tipp", "G_SDY_0121", "M_13_cis_retnglc_c", "R_24_25DHVITD2tm", "G_10090_AT1", "M_20ahchsterol_m",
      "R_1a_24_25VITD2Hm", "R_3DSPHR", "G_Acmsd", "M_12dgr_HP_c", "BIOMASS_HP_published", "M_26dap__M_c",
      "BIOMASS_Ecoli_TM", "G_S_0001", "M_26dap_LL_c", "BIOMASS_Ecoli_core_w_GAM", "G_test_mm_MM");
  private static List<BiGGId> testIds = new ArrayList<>();

  /**
   * Initializes BiGGId-Array for testing
   */
  @BeforeClass
  public static void setUp() {
    for (String id : ID_STRINGS) {
      testIds.add(new BiGGId(id));
    }
  }


  /**
   * Test method for {@link BiGGId#hashCode()}.
   */
  @Test
  public final void testHashCode() {
    assertEquals(923521, new BiGGId().hashCode());
    assertEquals(-2105880884, testIds.get(0).hashCode());
  }


  /**
   * Test method for
   * {@link BiGGId#equals(java.lang.Object)}.
   * Tests equality of both Constructors and the equals Method
   */
  @Test
  public final void testEqualsObject() {
    for (int idPos = 0; idPos < testIds.size(); idPos++) {
      BiGGId firstConstructorId = new BiGGId(ID_STRINGS.get(idPos));
      BiGGId testId = testIds.get(idPos);
      BiGGId secondConstructorId =
          new BiGGId(testId.getPrefix(), testId.getAbbreviation(), testId.getCompartmentCode(), testId.getTissueCode());
      assertEquals(testId, firstConstructorId);
      assertEquals(testId, secondConstructorId);
    }
  }


  /**
   * Test method for {@link BiGGId#isSetAbbreviation()}.
   */
  @Test
  public final void testIsSetAbbreviation() {
    for (BiGGId id : testIds) {
      assertTrue(id.isSetAbbreviation());
    }
  }


  /**
   * Test method for {@link BiGGId#isSetCompartmentCode()}.
   */
  @Test
  public final void testIsSetCompartmentCode() {
    assertFalse(new BiGGId().isSetCompartmentCode());
    List<Boolean> isSet = Arrays.asList(false, false, true, true, false, false, true, false, false, true, false, false,
        false, true, false, true, false, false, true, false, true);
    for (int i = 0; i < isSet.size(); i++) {
      assertEquals(isSet.get(i), testIds.get(i).isSetCompartmentCode());
    }
  }


  /**
   * Test method for {@link BiGGId#isSetPrefix()}.
   */
  @Test
  public final void testIsSetPrefix() {
    //TODO: cahnge according to new BiGGId implementation
    // for (BiGGId id : testIds) {
    //   assertTrue(id.isSetPrefix());
    // }
    assertFalse(new BiGGId().isSetPrefix());
  }


  /**
   * Test method for {@link BiGGId#isSetTissueCode()}.
   */
  @Test
  public final void testIsSetTissueCode() {
    assertFalse(new BiGGId().isSetTissueCode());
    for (int i = 0; i < 20; i++) {
      assertFalse(testIds.get(i).isSetTissueCode());
    }
    assertTrue(testIds.get(20).isSetTissueCode());
  }


  /**
   * Test method for
   * {@link BiGGId#setAbbreviation(java.lang.String)}.
   */
  @Test
  public final void testSetAbbreviation() {
    BiGGId oneTimeID = new BiGGId();
    oneTimeID.setAbbreviation("");
    assertEquals("", oneTimeID.getAbbreviation());
    assertEquals("26dap_LL", testIds.get(18).getAbbreviation());
  }


  /**
   * Test method for
   * {@link BiGGId#setCompartmentCode(java.lang.String)}
   * .
   */
  @Test
  public final void testSetCompartmentCode() {
    assertEquals("c", testIds.get(18).getCompartmentCode());
    assertEquals("mm", testIds.get(20).getCompartmentCode());
  }


  /**
   * Test method for
   * {@link BiGGId#setTissueCode(java.lang.String)}.
   */
  @Test
  public final void testSetTissueCode() {
    assertEquals("", testIds.get(18).getTissueCode());
    assertEquals("MM", testIds.get(20).getTissueCode());
  }


  /**
   * Test method for {@link BiGGId#toBiGGId()}.
   */
  @Test
  public final void testToBiGGId() {
    for (int idPos = 0; idPos < testIds.size(); idPos++) {
      assertEquals(ID_STRINGS.get(idPos), testIds.get(idPos).toBiGGId());
    }
  }


  @AfterClass
  public static void cleanUp() {
    testIds = null;
  }
}
