package edu.ucsd.sbrg.io.parsers.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.io.parsers.json.mapping.Compartments;
import edu.ucsd.sbrg.io.parsers.json.mapping.Metabolite;
import edu.ucsd.sbrg.io.parsers.json.mapping.Reaction;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.util.ModelBuilder;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JSONParserTest {

  private static ModelBuilder builder;
  private static final int LEVEL = 3;
  private static final int VERSION = 1;
  // TODO: add test for compartment parsing

  @BeforeEach
  public void setUp() {
    builder = new ModelBuilder(LEVEL, VERSION);
  }


  @Test
  public void parseCompartmentsTest() {
    String compartmentsJSON = """
            {
            "":"",
            "c":"cytoplasm",
            "C_c":"cytoplasm",
            "e":"extracellular",
            "w":"cell wall"
            }
            """;
    ObjectMapper mapper = new ObjectMapper();
    Compartments compartments = null;
    try {
      compartments = mapper.readValue(compartmentsJSON, Compartments.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    assertNotNull(compartments);
    JSONParser parser = new JSONParser(new IdentifiersOrg());
    parser.parseCompartments(builder, compartments.get());
    Model model = builder.getModel();
    assertEquals(3, model.getListOfCompartments().size());
    Compartment compartment = model.getCompartment("c");
    assertNotNull(compartment);
    assertEquals("cytoplasm", compartment.getName());
    compartment = model.getCompartment("e");
    assertNotNull(compartment);
    assertEquals("extracellular", compartment.getName());
    compartment = model.getCompartment("w");
    assertNotNull(compartment);
    assertEquals("cell wall", compartment.getName());
  }

  @Test
  public void parseReactionTest() {
    String reactionJSON = """
            {
            "id":"FAH1",
            "name":"Fatty acid omega-hydroxylase",
            "metabolites":{
            "ddca_c":-1.0,
            "h2o_c":1.0,
            "h_c":-1.0,
            "nadp_c":1.0,
            "nadph_c":-1.0,
            "o2_c":-1.0,
            "whddca_c":1.0
            },
            "lower_bound":1.0,
            "upper_bound":10.0,
            "gene_reaction_rule":"( 100767149 or 100767921 or 100768211 or 100768783 or 100769255 or 100755384 or 100773614 or 100750743 ) and 100689241",
            "subsystem":"dummy"
            }""";
    ObjectMapper mapper = new ObjectMapper();
    Reaction reaction = null;
    try {
      reaction = mapper.readValue(reactionJSON, Reaction.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    assertNotNull(reaction);
    JSONParser parser = new JSONParser(new IdentifiersOrg());
    BiGGId.createReactionId(reaction.getId()).ifPresentOrElse(id -> assertEquals("R_FAH1", id.toBiGGId()),
      Assertions::fail);
    parser.parseReaction(builder, reaction, "R_FAH1");
    // Needs to be fetched no matter what, checking for id equality is thus already done here indirectly
    org.sbml.jsbml.Reaction r = builder.getModel().getReaction("R_FAH1");
    assertNotNull(r);
    assertEquals("Fatty acid omega-hydroxylase", r.getName());
    FBCReactionPlugin fbc = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
    Parameter lb = fbc.getLowerFluxBoundInstance();
    assertNotNull(lb);
    assertEquals(1d, lb.getValue());
    Parameter ub = fbc.getUpperFluxBoundInstance();
    assertNotNull(ub);
    assertEquals(10d, ub.getValue());
    assertTrue(fbc.isSetGeneProductAssociation());
    assertFalse(r.isSetAnnotation());
    assertFalse(r.isSetNotes());
    List<SpeciesReference> reactants = new ArrayList<>();
    SpeciesReference sr = new SpeciesReference(LEVEL, VERSION);
    sr.setSpecies("M_ddca_c");
    sr.setStoichiometry(1d);
    sr.setConstant(true);
    reactants.add(sr);
    sr = new SpeciesReference(LEVEL, VERSION);
    sr.setSpecies("M_h_c");
    sr.setStoichiometry(1d);
    sr.setConstant(true);
    reactants.add(sr);
    sr = new SpeciesReference(LEVEL, VERSION);
    sr.setSpecies("M_nadph_c");
    sr.setStoichiometry(1d);
    sr.setConstant(true);
    reactants.add(sr);
    sr = new SpeciesReference(LEVEL, VERSION);
    sr.setSpecies("M_o2_c");
    sr.setStoichiometry(1d);
    sr.setConstant(true);
    reactants.add(sr);
    assertEquals(r.getNumReactants(), reactants.size());
    assertTrue(r.getListOfReactants().containsAll(reactants));
    List<SpeciesReference> products = new ArrayList<>();
    sr = new SpeciesReference(LEVEL, VERSION);
    sr.setSpecies("M_h2o_c");
    sr.setStoichiometry(1d);
    sr.setConstant(true);
    products.add(sr);
    sr = new SpeciesReference(LEVEL, VERSION);
    sr.setSpecies("M_nadp_c");
    sr.setStoichiometry(1d);
    sr.setConstant(true);
    products.add(sr);
    sr = new SpeciesReference(LEVEL, VERSION);
    sr.setSpecies("M_whddca_c");
    sr.setStoichiometry(1d);
    sr.setConstant(true);
    products.add(sr);
    assertEquals(r.getNumProducts(), products.size());
    assertTrue(r.getListOfProducts().containsAll(products));
    GroupsModelPlugin groupsModelPlugin = (GroupsModelPlugin) builder.getModel().getPlugin(GroupsConstants.shortLabel);
    List<String> groupNames =
      groupsModelPlugin.getListOfGroups().stream().map(AbstractSBase::getName).toList();
    assertEquals(1, groupNames.size());
    assertTrue(groupNames.contains("dummy"));
  }


  @Test
  public void parseSpeciesTest() {
    String speciesJSON = """
            {
              "id" : "amp_e",
              "name" : "AMP",
              "compartment" : "e",
              "charge" : -2,
              "formula" : "C10H12N5O7P"
            }
            """;
    ObjectMapper mapper = new ObjectMapper();
    Metabolite metabolite = null;
    try {
      metabolite = mapper.readValue(speciesJSON, Metabolite.class);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
    assertNotNull(metabolite);
    BiGGId.createMetaboliteId(metabolite.getId()).ifPresentOrElse(id -> assertEquals("M_amp_e", id.toBiGGId()),
      Assertions::fail);
    JSONParser parser = new JSONParser(new IdentifiersOrg());
    parser.parseMetabolite(builder.getModel(), metabolite, BiGGId.createMetaboliteId(metabolite.getId()).get());
    Species species = builder.getModel().getSpecies("M_amp_e");
    assertNotNull(species);
    assertEquals("AMP", metabolite.getName());
    assertEquals("e", metabolite.getCompartment());
    FBCSpeciesPlugin fbc = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
    assertEquals(-2d, fbc.getCharge());
    assertEquals("C10H12N5O7P", fbc.getChemicalFormula());
    assertFalse(species.isSetAnnotation());
    assertFalse(species.isSetNotes());
  }

  @Test
  public void iJB785isParsedWithoutError() throws XMLStreamException {
    var iJB785 = new File(JSONParserTest.class.getResource("iJB785.json").getFile());
    try {
        var sbmlDoc = new JSONParser(new IdentifiersOrg()).parse(iJB785);

      // see https://github.com/draeger-lab/ModelPolisher/issues/27 for context on this assertion
      var s =  sbmlDoc.getModel().getListOfSpecies()
              .stream()
              .filter(x -> x.getName().equals("Protein component of biomass"))
              .findFirst();
      assertTrue(s.isPresent(), "A formerly problematic metabolite in the model could not be found to be tested.");
      assertTrue(s.get().getNotesString().contains("H70.5616C44.9625O13.1713S0.2669N12.1054R-1.0"),
              "Formula from the model not retained in notes!");
    } catch (IOException e) {
      e.printStackTrace();
        fail("Parsing iJB785.json threw an exception.");
    }
  }
}
