/**
 * 
 */
package edu.ucsd.sbrg.util;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.Association;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.GeneProductAssociation;
import org.sbml.jsbml.ext.fbc.GeneProductRef;
import org.sbml.jsbml.ext.fbc.ListOfObjectives;
import org.sbml.jsbml.ext.fbc.LogicalOperator;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.groups.Member;

import javax.swing.tree.TreeNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A collection of helpful functions for dealing with SBML data structures.
 * 
 * @author Andreas Dr&auml;ger
 */
public class SBMLUtils {

  /**
   * A {@link Logger} for this class.
   */
  private static final Logger logger = Logger.getLogger(SBMLUtils.class.getName());
  /**
   * Key to link from {@link Reaction} directly to {@link Member}s referencing
   * that reaction.
   */
  public static final String SUBSYSTEM_LINK = "SUBSYSTEM_LINK";
  /**
   * A static map that holds references to all gene products within a model, facilitating quick updates.
   */
  private static Map<String, GeneProductRef> geneProductReferences = new HashMap<>();

  /**
   * Updates the reference of a gene product in the geneProductReferences map using the gene product's ID.
   * If the gene product ID starts with "G_", it strips this prefix before updating.
   * If the map is initially empty, it initializes the map with the current model's reactions.
   * 
   * @param gp The GeneProduct whose reference needs to be updated.
   */
  public static void updateGeneProductReference(GeneProduct gp) {
    if (geneProductReferences.isEmpty()) {
      initGPRMap(gp.getModel().getListOfReactions());
    }
    String id = gp.getId();
    if (id.startsWith("G_")) {
      id = id.split("G_")[1];
    }
    if (geneProductReferences.containsKey(id)) {
      GeneProductRef gpr = geneProductReferences.get(id);
      gpr.setGeneProduct(gp.getId());
    }
  }

  /**
   * Initializes the geneProductReferences map by traversing through all reactions and their associated gene products.
   * It handles both direct gene product references and nested logical operators that might contain gene product references.
   * 
   * @param reactions The list of reactions to scan for gene product associations.
   */
  private static void initGPRMap(ListOf<Reaction> reactions) {
    for (Reaction r : reactions) {
      for (int childIdx = 0; childIdx < r.getChildCount(); childIdx++) {
        TreeNode child = r.getChildAt(childIdx);
        if (child instanceof GeneProductAssociation) {
          Association association = ((GeneProductAssociation) child).getAssociation();
          if (association instanceof GeneProductRef) {
            GeneProductRef gpr = (GeneProductRef) association;
            geneProductReferences.put(gpr.getGeneProduct(), gpr);
          } else if (association instanceof LogicalOperator) {
            processNested(association);
          }
        }
      }
    }
  }

  /**
   * Clears the geneProductReferences map, removing all entries.
   */
  public static void clearGPRMap() {
    geneProductReferences = new HashMap<>();
  }


  /**
   * Recursively processes nested associations to map gene products to their references.
   * This method traverses through each child of the given association. If the child is a
   * LogicalOperator, it recursively processes it. If the child is a GeneProductRef, it
   * adds it to the geneProductReferences map.
   *
   * @param association The association to process, which can contain nested associations.
   */
  private static void processNested(Association association) {
    for (int idx = 0; idx < association.getChildCount(); idx++) {
      TreeNode child = association.getChildAt(idx);
      if (child instanceof LogicalOperator) {
        processNested((Association) child);
      } else {
        // This child is assumed to be a GeneProductRef
        GeneProductRef gpr = (GeneProductRef) child;
        geneProductReferences.put(gpr.getGeneProduct(), gpr);
      }
    }
  }

  /**
   * Updates the reaction reference ID from an old ID to a new ID within the FluxObjectives of a given FBCModelPlugin.
   * This method iterates through all objectives and their associated flux objectives in the model plugin,
   * replacing the old reaction ID with the new one wherever found.
   *
   * @param oldId The old reaction ID to be replaced.
   * @param newId The new reaction ID to replace the old one.
   * @param fbcModelPlug The FBCModelPlugin containing the objectives and flux objectives to be updated.
   */
  public static void updateReactionRef(String oldId, String newId, FBCModelPlugin fbcModelPlug) {
    if ((fbcModelPlug != null) && fbcModelPlug.isSetListOfObjectives()) {
      ListOfObjectives loo = fbcModelPlug.getListOfObjectives();
      for (Objective objective : loo) {
        if (objective.isSetListOfFluxObjectives()) {
          for (FluxObjective fo : objective.getListOfFluxObjectives()) {
            if (fo.getReaction().equals(oldId)) {
              fo.setReaction(newId);
            }
          }
        }
      }
    }
  }


  /**
   * Establishes a link between a reaction and a subsystem member by setting the member's reference to the reaction.
   * Additionally, it ensures that the reaction maintains a set of all members linked to it. If the set does not exist,
   * it is created and the member is added to it.
   * 
   * @param r The reaction object to which the member should be linked.
   * @param member The subsystem member that should be linked to the reaction.
   */
  @SuppressWarnings("unchecked")
  public static void createSubsystemLink(Reaction r, Member member) {
    // Set the member's reference ID to the reaction.
    member.setIdRef(r);
    // Check if the reaction has an existing set of members; if not, create one.
    if (r.getUserObject(SUBSYSTEM_LINK) == null) {
      r.putUserObject(SUBSYSTEM_LINK, new HashSet<Member>());
    }
    // Add the member to the reaction's set of linked members.
    ((Set<Member>) r.getUserObject(SUBSYSTEM_LINK)).add(member);
  }
}
