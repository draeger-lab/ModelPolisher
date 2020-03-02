package edu.ucsd.sbrg.util;

import de.zbit.util.Utils;
import edu.ucsd.sbrg.bigg.BiGGId;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.And;
import org.sbml.jsbml.ext.fbc.Association;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.fbc.GeneProductAssociation;
import org.sbml.jsbml.ext.fbc.GeneProductRef;
import org.sbml.jsbml.ext.fbc.LogicalOperator;
import org.sbml.jsbml.ext.fbc.Or;
import org.sbml.jsbml.text.parser.CobraFormulaParser;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static edu.ucsd.sbrg.bigg.ModelPolisher.mpMessageBundle;

public class GPRParser {

  /**
   * A {@link Logger} for this class.
   */
  private static final Logger logger = Logger.getLogger(GPRParser.class.getName());

  /**
   * @param r
   * @param geneReactionRule
   */
  public static void parseGPR(Reaction r, String geneReactionRule, boolean omitGenericTerms) {
    if ((geneReactionRule != null) && (geneReactionRule.length() > 0)) {
      Association association = null;
      try {
        association =
            convertToAssociation(ASTNode.parseFormula(geneReactionRule, new CobraFormulaParser(new StringReader(""))),
                r.getId(), r.getModel(), omitGenericTerms);
      } catch (Throwable exc) {
        logger.warning(
            MessageFormat.format(mpMessageBundle.getString("PARSE_GPR_ERROR"), geneReactionRule, Utils.getMessage(exc)));
      }
      if (association != null) {
        parseGPR(r, association, omitGenericTerms);
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
    identifier = BiGGId.createGeneId(identifier).toBiGGId();
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
    // get current association to replace
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
          Association child = (Association) association.getChildAt(idx);
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
}
