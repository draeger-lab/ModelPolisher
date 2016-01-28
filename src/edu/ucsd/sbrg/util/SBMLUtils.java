/**
 * 
 */
package edu.ucsd.sbrg.util;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.And;
import org.sbml.jsbml.ext.fbc.Association;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.GeneProductAssociation;
import org.sbml.jsbml.ext.fbc.GeneProductRef;
import org.sbml.jsbml.ext.fbc.ListOfObjectives;
import org.sbml.jsbml.ext.fbc.LogicalOperator;
import org.sbml.jsbml.ext.fbc.Objective;
import org.sbml.jsbml.ext.fbc.Or;
import org.sbml.jsbml.text.parser.CobraFormulaParser;

import de.zbit.util.Utils;


/**
 * @author Andreas Dr&auml;ger
 *
 */
public class SBMLUtils {

  /**
   * A {@link Logger} for this class.
   */
  private static final Logger logger = Logger.getLogger(SBMLUtils.class.getName());

  /**
   * 
   * @param ast
   * @param reactionId
   * @param model
   * @return
   */
  public static Association convertToAssociation(ASTNode ast, String reactionId, Model model, boolean omitGenericTerms) {
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
    GeneProductRef gpr = createGPR(ast.toString(), reactionId, model);
    return gpr;
  }

  /**
   * 
   * @param identifier
   * @param reactionId
   * @param model
   * @return
   */
  public static GeneProductRef createGPR(String identifier, String reactionId, Model model) {
    int level = model.getLevel(), version = model.getVersion();
    GeneProductRef gpr = new GeneProductRef(level, version);
    String id = SBMLUtils.updateGeneId(identifier);
    // check if this id exists in the model
    if (!model.containsUniqueNamedSBase(id)) {
      GeneProduct gp = (GeneProduct) model.findUniqueNamedSBase(identifier);
      if (gp == null) {
        logger.warning(MessageFormat.format("Creating missing gene product with id ''{0}'' because reaction ''{1}'' uses this id in its gene-product association.", id, reactionId));
        FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        gp = fbcPlug.createGeneProduct(id);
        gp.setLabel(id);
      } else {
        logger.info(MessageFormat.format("Updating the id of gene product ''{0}'' to ''{1}''.", gp.getId(), id));
        gp.setId(id);
      }
    }
    gpr.setGeneProduct(id);
    return gpr;
  }

  /**
   * @param r
   * @param geneReactionRule
   */
  public static void parseGPR(Reaction r, String geneReactionRule, boolean omitGenericTerms) {
    FBCReactionPlugin plugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
    if ((geneReactionRule != null) && (geneReactionRule.length() > 0)) {
      try {
        Association association = SBMLUtils.convertToAssociation(ASTNode.parseFormula(geneReactionRule, new CobraFormulaParser(new StringReader(""))), r.getId(), r.getModel(), omitGenericTerms);
        if (!plugin.isSetGeneProductAssociation() || !association.equals(plugin.getGeneProductAssociation().getAssociation())) {
          GeneProductAssociation gpa = new GeneProductAssociation(r.getLevel(), r.getVersion());
          gpa.setAssociation(association);
          plugin.setGeneProductAssociation(gpa);
        }
      } catch (Throwable exc) {
        logger.warning(MessageFormat.format("Could not parse ''{0}'' because of {1}", geneReactionRule, Utils.getMessage(exc)));
      }
    }
  }

  /**
   * 
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
   * 
   * @param geneProductAssociation
   * @return
   */
  public static Set<String> setOfGeneLinks(
    GeneProductAssociation geneProductAssociation) {
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
    id = id.replace("-", "_");
    //id = id.replace(".", "_AT");
    if (!id.startsWith("G_")) {
      id = "G_" + id;
    }
    return id;
  }

  /**
   * @param oldId
   * @param newId
   * @param fbcModelPlug
   */
  public static void updateReactionRef(String oldId, String newId,
    FBCModelPlugin fbcModelPlug) {
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
   * 
   * @param r
   */
  public static void setRequiredAttributes(Reaction r) {
    // TODO: make defaults user settings or take from L2V5.
    if (!r.isSetId()) {
      logger.severe(MessageFormat.format(
        "Found element of type {0} without defined id.", r.getElementName()));
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
    //    if (!r.isSetSBOTerm()) {
    //      r.setSBOTerm(SBO.getProcess());
    //    }
  }

}
