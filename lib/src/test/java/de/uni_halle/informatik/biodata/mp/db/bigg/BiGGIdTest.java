package de.uni_halle.informatik.biodata.mp.db.bigg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Thomas Zajac
 */
public class BiGGIdTest {

  private static Map<String, List<String>> biggIds = new HashMap<>();
  private static final Map<String, BiGGId> correctMetaboliteId = new HashMap<>();
  private static final Map<String, BiGGId> correctReactionId = new HashMap<>();
  private static final Map<String, BiGGId> correctGeneProductId = new HashMap<>();

  /**
   * Initializes BiGGIds for testing - empty IDs are should be handled elsewhere, as they are invalid in SBML
   */
  @BeforeAll
  @SuppressWarnings("unchecked")
  public static void setUp() throws IOException {
    // load all BiGG SBML IDs
    ObjectMapper mapper = new ObjectMapper();
    biggIds = mapper.readValue(BiGGId.class.getResourceAsStream("bigg_models_data_ids.json"), Map.class);
    // metabolites - unique cases from old implementation
    prepareMetaboliteId("h", "c", "h_c");
    prepareMetaboliteId("12dgr_HP", "c", "12dgr_HP_c");
    prepareMetaboliteId("13_cis_retnglc", "c", "13_cis_retnglc_c");
    prepareMetaboliteId("20ahchsterol", "m", "20ahchsterol_m");
    prepareMetaboliteId("26dap__M", "c", "26dap__M_c");
    prepareMetaboliteId("nadh", "c", "nadh_c");
    // metabolites - remaining cases
    prepareMetaboliteId("nadh", "", "nadh");
    prepareMetaboliteId("nadh____", "c", "nadh_____c");
    // reactions
    prepareReactionId("R", "2AGPE181tipp", "2AGPE181tipp");
    prepareReactionId("R", "24_25DHVITD2tm", "24_25DHVITD2tm");
    prepareReactionId("R", "3DSPHR", "3DSPHR");
    prepareReactionId("", "BIOMASS_Ecoli_core_w_GAM", "BIOMASS_Ecoli_core_w_GAM");
    prepareReactionId("", "BIOMASS_Ecoli_TM", "BIOMASS_Ecoli_TM");
    prepareReactionId("", "BIOMASS_HP_published", "BIOMASS_HP_published");
    // We don't handle reaction compartment code for now
    prepareReactionId("", "EX_h2o_e", "EX_h2o_e");
    // r reactions - remaining cases
    prepareReactionId("", "EX_h2o_e", "R_EX_h2o_e");
    // gene products
    prepareGeneProductId("10090_AT1", "10090_AT1");
    prepareGeneProductId("1818", "1818");
    prepareGeneProductId("1a_24_25VITD2Hm", "1a_24_25VITD2Hm");
    prepareGeneProductId("Acmsd", "Acmsd");
    prepareGeneProductId("SDY_0121", "SDY_0121");
    prepareGeneProductId("S_0001", "S_0001");
    // gene products reamining cases
    // prepareGeneProductId("","");
  }


  /**
   * Set up mapping for testIds to their corresponding correct BiGGId, test all four possibilites, i.e. w and w/o
   * prefix, lowercase prefix and prepended underscore
   *
   * @param abbreviation:
   *        Abbreviation part of id with no further semantic meaning
   * @param compartmentCode:
   *        CompartmentCode of id
   * @param id:
   *        Full id to test
   */
  private static void prepareMetaboliteId(String abbreviation, String compartmentCode, String id) {
    String prefix = "M";
    BiGGId biGGId = new BiGGId();
    biGGId.setPrefix(prefix);
    biGGId.setAbbreviation(abbreviation);
    biGGId.setCompartmentCode(compartmentCode);
    correctMetaboliteId.put(id, biGGId);
    correctMetaboliteId.put("_" + id, biGGId);
    correctMetaboliteId.put(prefix.toLowerCase() + "_" + id, biGGId);
    correctMetaboliteId.put(prefix + "_" + id, biGGId);
  }


  /**
   * Set up mapping for testIds to their corresponding correct BiGGId, test all four possibilites, i.e. w and w/o
   * prefix, lowercase prefix and prepended underscore
   *
   * @param prefix       :
   *                     Reaction prefix, if not pseudoreaction
   * @param abbreviation :
   *                     Abbreviation part of id with no further semantic meaning
   * @param id           :
   *                     Full id to test
   */
  private static void prepareReactionId(String prefix, String abbreviation, String id) {
    BiGGId biGGId = new BiGGId();
    biGGId.setPrefix(prefix);
    biGGId.setAbbreviation(abbreviation);
    biGGId.setCompartmentCode("");
    correctReactionId.put(id, biGGId);
    correctReactionId.put("_" + id, biGGId);
    // skip if pseudoreactions, produces duplicates of "_" + id for those
    if (!prefix.isEmpty()) {
      correctReactionId.put(prefix.toLowerCase() + "_" + id, biGGId);
      correctReactionId.put(prefix + "_" + id, biGGId);
    }
  }


  /**
   * Set up mapping for testIds to their corresponding correct BiGGId, test all four possibilites, i.e. w and w/o
   * prefix, lowercase prefix and prepended underscore
   *
   * @param abbreviation:
   *        Abbreviation part of id with no further semantic meaning
   * @param id:
   *        Full id to test
   */
  private static void prepareGeneProductId(String abbreviation, String id) {
    String prefix = "G";
    BiGGId biGGId = new BiGGId();
    biGGId.setPrefix(prefix);
    biGGId.setAbbreviation(abbreviation);
    correctGeneProductId.put(id, biGGId);
    correctGeneProductId.put("_" + id, biGGId);
    correctGeneProductId.put(prefix.toLowerCase() + "_" + id, biGGId);
    correctGeneProductId.put(prefix + "_" + id, biGGId);
  }


  @Test
  public final void geneIdsValid() {
    List<String> ids = biggIds.get("genes");
    ids.forEach(id -> assertTrue(BiGGId.isValid(BiGGId.createGeneId(id).toBiGGId())));
  }


  @Test
  public final void metaboliteIdsValid() {
    List<String> ids = biggIds.get("metabolites");
    ids.forEach(id -> assertTrue(BiGGId.isValid(BiGGId.createGeneId(id).toBiGGId())));
  }


  @Test
  public final void reactionIdsValid() {
    List<String> ids = biggIds.get("reactions");
    ids.forEach(id -> assertTrue(BiGGId.isValid(BiGGId.createGeneId(id).toBiGGId())));
  }


  @Test
  public final void testIsSetMetaboliteCompartment() {
    String metabolite = "M_5dglcn_c";
    BiGGId biggId = BiGGId.createMetaboliteId(metabolite);
    assertTrue(biggId.isSetCompartmentCode());
    assertEquals("c", biggId.getCompartmentCode());
  }


  /**
   * Test method for {@link BiGGId#hashCode()}.
   */
  @Test
  public final void testHashCode() {
    assertEquals(923521, new BiGGId().hashCode());
  }


  /**
   * Test method for {@link BiGGId#toBiGGId()} for geneProduct ids
   */
  @Test
  public final void testToBiGGIdGeneProducts() {
    for (Map.Entry<String, BiGGId> entry : correctGeneProductId.entrySet()) {
      var id = BiGGId.createGeneId(entry.getKey());
      assertEquals(id.toBiGGId(), entry.getValue().toBiGGId());
    }
  }


  /**
   * Test method for {@link BiGGId#toBiGGId()} for metabolite ids
   */
  @Test
  public final void testToBiGGIdMetabolites() {
    for (Map.Entry<String, BiGGId> entry : correctMetaboliteId.entrySet()) {
      var id = BiGGId.createMetaboliteId(entry.getKey());
      assertEquals(id.toBiGGId(), entry.getValue().toBiGGId());
    }
  }


  /**
   * Test method for {@link BiGGId#toBiGGId()} for reaction ids
   */
  @Test
  public final void testToBiGGIdReactions() {
    for (Map.Entry<String, BiGGId> entry : correctReactionId.entrySet()) {
      var id = BiGGId.createReactionId(entry.getKey());
      assertEquals(id.toBiGGId(), entry.getValue().toBiGGId());
    }
  }


}
