package edu.ucsd.sbrg.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.GeneProductAssociation;
import org.sbml.jsbml.ext.fbc.GeneProductRef;

public class GPRParserTest {

  final static int LEVEL = 3;
  final static int VERSION = 1;
  static Model model = new Model(LEVEL, VERSION);
  static List<String> geneReactionRules = new ArrayList<>();
  static List<GeneProductAssociation> geneProductAssociations = new ArrayList<>();

  @BeforeAll
  public static void setUp() {
    String[] grr = {"1591.1", "8639.1 or 26.1 or 314.2 or 314.1",
      "(4967.2 and 1738.1 and 8050.1 and 1743.1) or (4967.1 and 1738.1 and 8050.1 and 1743.1)",
      "130.1 or 127.1 or (125.1 and 124.1) or 131.1 or (126.1 and 124.1) or 128.1 or 137872.1 or (125.1 and 126.1)"};
    geneReactionRules.addAll(Arrays.asList(grr));
    //geneProductAssociations.add(prepareFirstResult());
  }


  private static void prepareFirstResult() {
    GeneProductRef geneProductRef = new GeneProductRef(LEVEL, VERSION);
    FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    String identifier = "G_1591_AT1";
    GeneProduct geneProduct = fbcPlug.createGeneProduct(identifier);
    geneProduct.setLabel(identifier);
    geneProductRef.setGeneProduct(identifier);
    //return (GeneProductAssociation) geneProductRef;
  }


  private static GeneProductAssociation prepareSecondResult() {
    GeneProductAssociation association = new GeneProductAssociation(LEVEL, VERSION);
    GeneProductRef geneProductRef = new GeneProductRef(LEVEL, VERSION);
    FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    String identifier = "G_1591_AT1";
    GeneProduct gp = fbcPlug.createGeneProduct(identifier);
    gp.setLabel(identifier);
    return association;
  }


  public final void test() {
  }
}
