package edu.ucsd.sbrg.util.ext.fbc;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import de.zbit.util.Utils;
import org.sbml.jsbml.*;
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

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import org.sbml.jsbml.text.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.text.MessageFormat.format;


public class GPRParser {

  private static final Logger logger = LoggerFactory.getLogger(GPRParser.class);
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");


  public static void setGeneProductAssociation(Reaction r, String geneReactionRule, boolean addGenericTerms) {
    try {
      ASTNode ast = ASTNode.parseFormula(
              geneReactionRule,
              new CobraFormulaParser(new StringReader("")));

      Association association = convertToAssociation(
              ast,
              r.getModel(),
              addGenericTerms);

      var reactionPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
      if (!reactionPlugin.isSetGeneProductAssociation()) {
        var gpa = new GeneProductAssociation(r.getLevel(), r.getVersion());
        gpa.setAssociation(association);
        reactionPlugin.setGeneProductAssociation(gpa);
      } else if (!areEqual(association, reactionPlugin.getGeneProductAssociation().getAssociation())) {
        mergeAssociation(r, association, reactionPlugin, addGenericTerms);
      }
    } catch (ParseException e) {
      logger.info(format(MESSAGES.getString("PARSE_GPR_ERROR"), geneReactionRule, Utils.getMessage(e)));
    }
  }


  private static Association convertToAssociation(ASTNode ast, Model model, boolean addGenericTerms) {
    int level = model.getLevel(), version = model.getVersion();
    if (ast.isLogical()) {
      LogicalOperator operator;
      if (ast.getType() == ASTNode.Type.LOGICAL_AND) {
        operator = new And(level, version);
        if (addGenericTerms) {
          operator.setSBOTerm(173); // AND
        }
      } else {
        operator = new Or(level, version);
        if (addGenericTerms) {
          operator.setSBOTerm(174); // OR
        }
      }
      for (ASTNode child : ast.getListOfNodes()) {
        Association tmp = convertToAssociation(child, model, addGenericTerms);
        if (tmp.getClass().equals(operator.getClass())) {
          // Flatten binary trees to compact representation
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
    return createGPR(ast.toString(), model);
  }


  private static GeneProductRef createGPR(String astString, Model model) {
    // Determine the SBML document level and version for creating new elements.
    int level = model.getLevel(), version = model.getVersion();
    var gpr = new GeneProductRef(level, version);

    // Normalize the identifier to include "G_" prefix if missing.
    String oldId = astString.startsWith("G_") ? astString : "G_" + astString;
    boolean containsOldId = model.containsUniqueNamedSBase(oldId);

    // Attempt to create or find the GeneProduct using a standardized identifier.
    var id = BiGGId.createGeneId(astString).toBiGGId();
    if (!model.containsUniqueNamedSBase(id)) {
      // Check if the old ID exists, if so, retrieve the GeneProduct, otherwise use the new ID.
      if (containsOldId) {
        var gp = (GeneProduct) model.findUniqueNamedSBase(oldId);
        gp.setId(id);
      } else {
        var fbcPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        var gp = fbcPlug.createGeneProduct(id);
        gp.setLabel(id);
      }
    }
    // Set the GeneProduct reference in the GeneProductRef.
    gpr.setGeneProduct(id);
    return gpr;
  }


  private static void mergeAssociation(Reaction r,
                                       Association association,
                                       FBCReactionPlugin reactionPlugin,
                                       boolean addGenericTerms) {
    // get current association to replace
    var oldGpa = reactionPlugin.getGeneProductAssociation().getAssociation();
    var gpa = new GeneProductAssociation(r.getLevel(), r.getVersion());
    // link all GPRs fetched with or
    LogicalOperator or = new Or(r.getLevel(), r.getVersion());
    if (addGenericTerms) {
      or.setSBOTerm(174); // OR
    }
    if (oldGpa instanceof And) {
      or.addAssociation(oldGpa);
      or.addAssociation(association);
      Set<String> gprs = new HashSet<>();
      for (int i = 0; i < or.getChildCount(); i++) {
        Association current = (Association) or.getChildAt(i);
        if (current instanceof GeneProductRef) {
          String geneProduct = ((GeneProductRef) current).getGeneProduct();
          if (gprs.contains(geneProduct)) {
            if (!or.removeAssociation(current)) {
              logger.info(format("Failed to unset duplicate GeneProductReference {0} for reaction {1}",
                      geneProduct, r.getId()));
            }
          } else {
            gprs.add(geneProduct);
          }
        }
      }
      gpa.setAssociation(or);
    } else if (oldGpa instanceof GeneProductRef) {
      if (association instanceof Or) {
        or = (Or) association;
      } else {
        or.addAssociation(association);
      }
      or.addAssociation(oldGpa);
      gpa.setAssociation(or);
    } else { // OR
      if (association instanceof Or) {
        for (int idx = 0; idx < association.getChildCount(); idx++) {
          Association child = (Association) association.getChildAt(idx);
          ((Or) association).removeAssociation(idx);
          ((LogicalOperator) oldGpa).addAssociation(child);
        }
      } else {
        ((LogicalOperator) oldGpa).addAssociation(association);
      }
      gpa.setAssociation(oldGpa);
    }
    reactionPlugin.setGeneProductAssociation(gpa);
  }

  private static boolean areEqual(Association association1, Association association2) {
    if (association1.isLeaf() && association2.isLeaf()) {
      return true;
    } else if (association1 instanceof Or && association2 instanceof Or) {
      if (((Or) association1).getNumChildren() != ((Or) association2).getNumChildren()) {
        return false;
      } else {
        boolean childrenEqual = true;
        for (int i = 0; i < ((Or) association1).getNumChildren(); i++) {
          childrenEqual &= areEqual((Association) association1.getChildAt(i), (Association) association2.getChildAt(i));
        }
        return childrenEqual;
      }
    } else if (association1 instanceof And && association2 instanceof And) {
      if (((And) association1).getNumChildren() != ((And) association2).getNumChildren()) {
        return false;
      } else {
        boolean childrenEqual = true;
        for (int i = 0; i < ((And) association1).getNumChildren(); i++) {
          childrenEqual &= areEqual((Association) association1.getChildAt(i), (Association) association2.getChildAt(i));
        }
        return childrenEqual;
      }
    }
    return false;
  }


  public static String stringify(Association association) {
    if (association instanceof GeneProductRef) {
      // Directly return the gene product identifier for GeneProductRef instances.
      return ((GeneProductRef) association).getGeneProduct();
    } else if (association instanceof And) {
      // Handle the 'And' type association by iterating over its children.
      List<Association> children = ((And) association).getListOfAssociations();
      int numChildren = ((And) association).getAssociationCount();
      StringBuilder sb = new StringBuilder();
      sb.append("(").append(stringify(children.get(0))).append(")");
      for (int i = 1; i < numChildren; i++) {
        sb.append(" and (").append(stringify(children.get(i))).append(")");
      }
      return sb.toString();
    } else {
      // Handle the 'Or' type association by iterating over its children.
      List<Association> children = ((Or) association).getListOfAssociations();
      int numChildren = ((Or) association).getAssociationCount();
      StringBuilder sb = new StringBuilder();
      sb.append("(").append(stringify(children.get(0))).append(")");
      for (int i = 1; i < numChildren; i++) {
        sb.append(" or (").append(stringify(children.get(i))).append(")");
      }
      return sb.toString();
    }
  }





}
