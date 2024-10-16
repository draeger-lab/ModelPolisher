package de.uni_halle.informatik.biodata.mp.io.parsers.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.GeneProductAssociation;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.ext.groups.Member;
import org.sbml.jsbml.xml.XMLNode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import de.uni_halle.informatik.biodata.mp.db.bigg.BiGGId;
import de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Compartments;
import de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Gene;
import de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Metabolite;
import de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Metabolites;
import de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Root;
import de.uni_halle.informatik.biodata.mp.util.ext.fbc.GPRParser;

public class JSONConverter {
  private static Map<String, String> compartments = new HashMap<>();


  public static Root convertModel(Model model) throws XMLStreamException {
    Root root = new Root();
    root.setId(model.getId());
    root.setName(model.getName());
    root.setVersion(model.getVersion());
    compartments = new HashMap<>();
    // description is currently empty
    root.setReactions(convertReactions(model));
    root.setMetabolites(convertMetabolites(model));
    root.setGenes(convertGenes(model));
    if (!compartments.isEmpty()) {
      Compartments comps = new Compartments();
      comps.addAll(compartments);
      root.setCompartments(comps);
    }
    if (model.isSetNotes()) {
      root.setNotes(serializeNotes(model.getNotes()));
    }
    root.setAnnotation(serializeAnnotation(model.getAnnotation()));
    return root;
  }


  public static List<Gene> convertGenes(Model model) throws XMLStreamException {
    List<Gene> genes = new ArrayList<>();
    FBCModelPlugin modelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
    for (GeneProduct g : modelPlug.getListOfGeneProducts()) {
      genes.add(convertGene(g));
    }
    return genes;
  }


  public static Gene convertGene(GeneProduct g) throws XMLStreamException {
    Gene gene = new Gene();
    gene.setId(g.getId());
    gene.setName(g.getName());
    if (g.isSetNotes()) {
      gene.setNotes(serializeNotes(g.getNotes()));
    }
    gene.setAnnotation(serializeAnnotation(g.getAnnotation()));
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    return gene;
  }


  public static List<Metabolite> convertMetabolites(Model model) throws XMLStreamException {
    List<Metabolite> metabolites = new ArrayList<>();
    for (Species species : model.getListOfSpecies()) {
      metabolites.add(convertMetabolite(species));
    }
    return metabolites;
  }


  public static Metabolite convertMetabolite(Species species) throws XMLStreamException {
    Metabolite metabolite = new Metabolite();
    metabolite.setId(species.getId());
    metabolite.setName(species.getName());
    if (species.isSetCompartment()) {
      String compartment = species.getCompartment();
      var biGGId = BiGGId.createMetaboliteId(species.getId());
      String compartmentCode = biGGId.getCompartmentCode();
      if (!compartmentCode.isEmpty() && !compartments.containsKey(compartmentCode)) {
        compartments.put(compartmentCode, compartment);
      }
      metabolite.setCompartment(compartment);
    } else {
      metabolite.setCompartment("");
    }
    FBCSpeciesPlugin specPlug = (FBCSpeciesPlugin) species.getPlugin(FBCConstants.shortLabel);
    if (specPlug.isSetCharge()) {
      metabolite.setCharge(specPlug.getCharge());
    }
    if (specPlug.isSetChemicalFormula()) {
      metabolite.setFormula(specPlug.getChemicalFormula());
    }
    if (species.isSetNotes()) {
      metabolite.setNotes(serializeNotes(species.getNotes()));
    }
    metabolite.setAnnotation(serializeAnnotation(species.getAnnotation()));
    return metabolite;
  }


  public static List<de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Reaction> convertReactions(Model model) throws XMLStreamException {
    List<de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Reaction> reactions = new ArrayList<>();
    for (Reaction reaction : model.getListOfReactions()) {
      reactions.add(convertReaction(reaction));
    }
    return reactions;
  }


  public static de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Reaction convertReaction(Reaction r) throws XMLStreamException {
    de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Reaction reaction = new de.uni_halle.informatik.biodata.mp.io.parsers.json.mapping.Reaction();
    reaction.setId(r.getId());
    reaction.setName(r.getName());
    Metabolites metabolites = new Metabolites();
    r.getListOfReactants().forEach(reference -> metabolites.add(reference.getSpecies(),
      reference.getStoichiometry() < 0 ? reference.getStoichiometry() : reference.getStoichiometry() * -1));
    r.getListOfProducts().forEach(reference -> metabolites.add(reference.getSpecies(), reference.getStoichiometry()));
    reaction.setMetabolites(metabolites);
    FBCReactionPlugin reactionPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
    if (reactionPlugin.isSetGeneProductAssociation()) {
      GeneProductAssociation gpa = reactionPlugin.getGeneProductAssociation();
      reaction.setGeneReactionRule(GPRParser.stringify(gpa.getAssociation()));
    } else {
      reaction.setGeneReactionRule("");
    }
    boolean reversible = r.isReversible();
    Parameter lb = reactionPlugin.getLowerFluxBoundInstance();
    double lbVal;
    if (reversible) {
      lbVal = -1000d;
    } else {
      lbVal = 0d;
    }
    if (lb != null) {
      lbVal = lb.getValue();
    }
    reaction.setLowerBound(lbVal);
    Parameter ub = reactionPlugin.getUpperFluxBoundInstance();
    double ubVal = 1000;
    if (ub != null) {
      ubVal = ub.getValue();
    }
    reaction.setUpperBound(ubVal);
    FBCModelPlugin modelPlugin = (FBCModelPlugin) r.getModel().getPlugin(FBCConstants.shortLabel);
    Objective obj = modelPlugin.getObjective(0);
    if (obj != null) {
      for (FluxObjective fo : obj.getListOfFluxObjectives()) {
        if (fo.getReactionInstance().equals(r)) {
          reaction.setObjectiveCoefficient(fo.getCoefficient());
          break;
        }
      }
    } else {
      reaction.setObjectiveCoefficient(0d);
    }
    // Set reaction subsystem to first member match
    // experimental: is this correct if multiple member objects and/or multiple matches could exist potentially?
    Object subsystem = r.getUserObject("SUBSYSTEM_LINK");
    if (subsystem instanceof Set<?>) {
      for (Object member : (Set<?>) subsystem) {
        if (member instanceof Member) {
          GroupsModelPlugin groupsModelPlugin = (GroupsModelPlugin) r.getModel().getPlugin(GroupsConstants.shortLabel);
          for (Group group : groupsModelPlugin.getListOfGroups()) {
            for (Member groupMember : group.getListOfMembers()) {
              if (member.equals(groupMember)) {
                reaction.setSubsystem(group.getName());
                break;
              }
            }
          }
        }
      }
    }
    if (r.isSetNotes()) {
      reaction.setNotes(serializeNotes(r.getNotes()));
    }
    reaction.setAnnotation(serializeAnnotation(r.getAnnotation()));
    return reaction;
  }


  public static String getJSONDocument(SBMLDocument doc) throws JsonProcessingException, XMLStreamException {
    return getJSONModel(doc.getModel());
  }


  public static String getJSONModel(Model model) throws JsonProcessingException, XMLStreamException {
    Root root = convertModel(model);
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    return mapper.writeValueAsString(root);
  }


  private static Map<String, List<String>> serializeAnnotation(Annotation annotation) {
    Map<String, List<String>> terms = new LinkedHashMap<>();
    for (CVTerm term : annotation.getListOfCVTerms()) {
      terms.put(term.getQualifier().getElementNameEquivalent(), term.getResources());
    }
    return terms;
  }


  private static List<String> serializeNotes(XMLNode notes) throws XMLStreamException {
    List<String> convertedNotes = new ArrayList<>();
    int numChildren = notes.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      XMLNode child = notes.getChild(i);
      for (int j = 0; j < child.getChildCount(); j++) {
        XMLNode leaf = child.getChild(j);
        // remove XML tags and whitespace
        String text = leaf.toXMLString().replaceAll("<.*?>", "").strip();
        if (!text.isEmpty()) {
          convertedNotes.add(text);
        }
      }
    }
    return convertedNotes;
  }
}
