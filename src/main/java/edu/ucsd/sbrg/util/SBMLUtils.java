/**
 * 
 */
package edu.ucsd.sbrg.util;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.tree.TreeNode;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.ext.groups.Member;
import org.sbml.jsbml.text.parser.CobraFormulaParser;

import de.zbit.util.Utils;

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

  // TODO: update for BiGGId changes
  /**
   * @param r
   * @param geneReactionRule
   */
  public static void parseGPR(Reaction r, String geneReactionRule, boolean omitGenericTerms) {
    if ((geneReactionRule != null) && (geneReactionRule.length() > 0)) {
      try {
        Association association = SBMLUtils.convertToAssociation(
          ASTNode.parseFormula(geneReactionRule, new CobraFormulaParser(new StringReader(""))), r.getId(), r.getModel(),
          omitGenericTerms);
        parseGPR(r, association, omitGenericTerms);
      } catch (Throwable exc) {
        logger.warning(
          MessageFormat.format(mpMessageBundle.getString("PARSE_GPR_ERROR"), geneReactionRule, Utils.getMessage(exc)));
      }
    }
  }


  /**
   * @param ast
   * @param reactionId
   * @param model
   * @return
   */
  public static Association convertToAssociation(ASTNode ast, String reactionId, Model model,
    boolean omitGenericTerms) {
    int level = model.getLevel(), version = model.getVersion();
    if (ast.isLogical()) {
      LogicalOperator operator;
      if (ast.getType() == ASTNode.Type.LOGICAL_AND) {
        operator = new And(level, version);
        if (!omitGenericTerms) {
          operator.setSBOTerm(173); // AND
        }
      } else {
        operator = new Or(level, version);
        if (!omitGenericTerms) {
          operator.setSBOTerm(174); // OR
        }
      }
      for (ASTNode child : ast.getListOfNodes()) {
        Association tmp = convertToAssociation(child, reactionId, model, omitGenericTerms);
        if (tmp.getClass().equals(operator.getClass())) {
          // flatten binary trees to compact representation
          LogicalOperator lo = (LogicalOperator) tmp;
          for (int i = lo.getAssociationCount() - 1; i >= 0; i--) {
            operator.addAssociation(lo.removeAssociation(i));
          }
        } else {
          operator.addAssociation(tmp);
        }
      }
      return operator;
    }
    return createGPR(ast.toString(), reactionId, model);
  }


  /**
   * @param identifier
   * @param reactionId
   * @param model
   * @return
   */
  public static GeneProductRef createGPR(String identifier, String reactionId, Model model) {
    int level = model.getLevel(), version = model.getVersion();
    GeneProductRef gpr = new GeneProductRef(level, version);
    // check if this id exists in the model
    identifier = updateGeneId(identifier);
    if (!model.containsUniqueNamedSBase(identifier)) {
      GeneProduct gp = (GeneProduct) model.findUniqueNamedSBase(identifier);
      if (gp == null) {
        logger.warning(MessageFormat.format(mpMessageBundle.getString("CREATE_MISSING_GPR"), identifier, reactionId));
        FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        gp = fbcPlug.createGeneProduct(identifier);
        gp.setLabel(identifier);
      } else {
        logger.info(MessageFormat.format(mpMessageBundle.getString("UPDATE_GP_ID"), gp.getId(), identifier));
        gp.setId(identifier);
      }
    }
    gpr.setGeneProduct(identifier);
    return gpr;
  }


  /**
   * @param r
   * @param association
   * @param omitGenericTerms
   */
  private static void parseGPR(Reaction r, Association association, boolean omitGenericTerms) {
    FBCReactionPlugin plugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
    if (!plugin.isSetGeneProductAssociation()) {
      GeneProductAssociation gpa = new GeneProductAssociation(r.getLevel(), r.getVersion());
      gpa.setAssociation(association);
      plugin.setGeneProductAssociation(gpa);
    } else if (!association.equals(plugin.getGeneProductAssociation().getAssociation())) {
      mergeAssociation(r, association, plugin, omitGenericTerms);
    }
  }


  /**
   * @param r
   * @param association
   * @param plugin
   * @param omitGenericTerms
   */
  private static void mergeAssociation(Reaction r, Association association, FBCReactionPlugin plugin,
    boolean omitGenericTerms) {
    Association old_association = plugin.getGeneProductAssociation().getAssociation();
    plugin.getGeneProductAssociation().unsetAssociation();
    GeneProductAssociation gpa = new GeneProductAssociation(r.getLevel(), r.getVersion());
    // link all GPRs fetched with or
    LogicalOperator or = new Or(r.getLevel(), r.getVersion());
    if (!omitGenericTerms) {
      or.setSBOTerm(174); // OR
    }
    if (old_association instanceof And) {
      or.addAssociation(old_association);
      or.addAssociation(association);
      gpa.setAssociation(or);
    } else if (old_association instanceof GeneProductRef) {
      if (association instanceof Or) {
        or = (Or) association;
      } else {
        or.addAssociation(association);
      }
      or.addAssociation(old_association);
      gpa.setAssociation(or);
    } else { // OR
      if (association instanceof Or) {
        for (int idx = 0; idx < association.getChildCount(); idx++) {
          GeneProductRef child = (GeneProductRef) association.getChildAt(idx);
          ((Or) association).removeAssociation(idx);
          ((LogicalOperator) old_association).addAssociation(child);
        }
      } else {
        ((LogicalOperator) old_association).addAssociation(association);
      }
      gpa.setAssociation(old_association);
    }
    plugin.setGeneProductAssociation(gpa);
  }


  /**
   * @param association
   * @return
   */
  public static Set<String> setOfGeneLinks(Association association) {
    Set<String> links = new HashSet<>();
    if (association instanceof GeneProductRef) {
      links.add(((GeneProductRef) association).getGeneProduct());
    } else {
      LogicalOperator operator = (LogicalOperator) association;
      for (int i = 0; i < operator.getChildCount(); i++) {
        links.addAll(setOfGeneLinks(operator.getAssociation(i)));
      }
    }
    return links;
  }


  /**
   * @param geneProductAssociation
   * @return
   */
  public static Set<String> setOfGeneLinks(GeneProductAssociation geneProductAssociation) {
    if (geneProductAssociation.isSetAssociation()) {
      return setOfGeneLinks(geneProductAssociation.getAssociation());
    }
    return new HashSet<String>();
  }
  
  /**
   * @param id
   * @return
   */
  public static String updateGeneId(String id) {
    if (!id.startsWith("G_")) {
     id = "G_" + id;
    }
    if (id.contains(".")) {
      id = id.replaceAll("\\.", "_DOT_");
    }
    return id;
  }


  /**
   * Apply updated GeneID to geneProductReferenece
   * 
   * @param gp
   */
  public static void updateGeneProductReference(GeneProduct gp) {
    String id = gp.getId();
    if (id.startsWith("G_")) {
      id = id.split("G_")[1];
    }
    // does findUniqueSBase work here?
    for (Reaction r : gp.getModel().getListOfReactions()) {
      for (int childIdx = 0; childIdx < r.getChildCount(); childIdx++) {
        TreeNode child = r.getChildAt(childIdx);
        if (child instanceof GeneProductAssociation) {
          Association association = ((GeneProductAssociation) child).getAssociation();
          if (association instanceof GeneProductRef) {
            GeneProductRef gpr = (GeneProductRef) association;
            if (id.equals(gpr.getGeneProduct())) {
              gpr.setGeneProduct(gp.getId());
            }
          } else if (association instanceof LogicalOperator) {
            processNested(association, gp, id);
          }
        }
      }
    }
  }


  /**
   * @param association
   * @param gp
   * @param id
   */
  private static void processNested(Association association, GeneProduct gp, String id) {
    for (int idx = 0; idx < association.getChildCount(); idx++) {
      TreeNode child = association.getChildAt(idx);
      if (child instanceof LogicalOperator) {
        processNested((Association) child, gp, id);
      } else {
        // has to GeneProductReference
        GeneProductRef gpr = (GeneProductRef) child;
        if (id.equals(gpr.getGeneProduct())) {
          // if should be unique
          gpr.setGeneProduct(gp.getId());
          break;
        }
      }
    }
  }


  /**
   * @param oldId
   * @param newId
   * @param fbcModelPlug
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
   * @param r
   */
  public static void setRequiredAttributes(Reaction r) {
    // TODO: make defaults user settings or take from L2V5.
    if (!r.isSetId()) {
      logger.severe(MessageFormat.format(mpMessageBundle.getString("ID_MISSING_FOR_TYPE"), r.getElementName()));
    }
    if (!r.isSetFast()) {
      r.setFast(false);
    }
    if (!r.isSetReversible()) {
      r.setReversible(false);
    }
    if (!r.isSetMetaId() && ((r.getCVTermCount() > 0) || r.isSetHistory())) {
      r.setMetaId(r.getId());
    }
    // TODO
    // if (!r.isSetSBOTerm()) {
    // r.setSBOTerm(SBO.getProcess());
    // }
  }


  /**
   * Add a direct link from the reaction to the member pointing to that
   * reaction.
   * 
   * @param r
   * @param member
   */
  @SuppressWarnings("unchecked")
  public static void createSubsystemLink(Reaction r, Member member) {
    member.setIdRef(r);
    if (r.getUserObject(SUBSYSTEM_LINK) == null) {
      r.putUserObject(SUBSYSTEM_LINK, new HashSet<Member>());
    }
    ((Set<Member>) r.getUserObject(SUBSYSTEM_LINK)).add(member);
  }
}
