package edu.ucsd.sbrg.util;

import static java.text.MessageFormat.format;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

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
import de.zbit.util.Utils;
import edu.ucsd.sbrg.db.bigg.BiGGId;

/**
 * The {@code GPRParser} class provides methods to parse gene product associations (GPRs) from gene reaction rules
 * and integrate them into SBML models using JSBML. It supports converting textual gene reaction rules into structured
 * {@link Association} objects, handling logical operators, and merging associations into existing models.
 * It also includes utilities for converting associations to the FBC v2 format and managing gene product references.
 *
 * <p>This class is designed to be used in scenarios where gene reaction rules need to be parsed from various formats
 * and integrated into computational models in a structured and standardized form. It provides comprehensive support
 * for handling complex logical structures in gene product associations, such as nested AND/OR conditions.</p>
 *
 * <p>Utility methods in this class are static, allowing direct invocation without needing an instance of {@code GPRParser}.
 * This class heavily relies on the JSBML library to manipulate elements of SBML files, particularly those related to
 * the FBC (Flux Balance Constraints) package.</p>
 */
public class GPRParser {

  /**
   * A {@link Logger} for this class.
   */
  private static final Logger logger = Logger.getLogger(GPRParser.class.getName());
  /**
   * Bundle for ModelPolisher logger messages
   */
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");


  /**
   * Parses the gene product association (GPR) from a gene reaction rule string and associates it with a given reaction.
   * This method first converts the gene reaction rule string into an Association object using a formula parser.
   * If the conversion is successful and the Association object is not null, it further processes the association
   * by parsing it into the reaction's gene product association.
   *
   * @param r The reaction to which the gene product association will be linked.
   * @param geneReactionRule The gene reaction rule string representing the association of gene products.
   * @param omitGenericTerms Flag indicating whether to omit generic terms (e.g., SBO terms) in the association.
   */
  public static void parseGPR(Reaction r, String geneReactionRule, boolean omitGenericTerms) {
    if ((geneReactionRule != null) && (!geneReactionRule.isEmpty())) {
      Association association = null;
      try {
        association =
          convertToAssociation(ASTNode.parseFormula(geneReactionRule, new CobraFormulaParser(new StringReader(""))),
            r.getId(), r.getModel(), omitGenericTerms);
      } catch (Throwable exc) {
        logger.warning(format(MESSAGES.getString("PARSE_GPR_ERROR"), geneReactionRule, Utils.getMessage(exc)));
      }
      if (association != null) {
        parseGPR(r, association, omitGenericTerms);
      }
    }
  }


  /**
   * Converts an ASTNode representing a gene product association into an Association object.
   * This method handles the logical structure of the gene product association, creating appropriate
   * logical operators (AND, OR) based on the ASTNode type. It also manages the inclusion of SBO terms
   * if they are not omitted.
   *
   * @param ast The ASTNode to be converted, representing the logical structure of the gene product association.
   * @param reactionId The ID of the reaction associated with this gene product association.
   * @param model The SBML model to which the reaction belongs, used to determine the level and version for new elements.
   * @param omitGenericTerms A boolean flag indicating whether to omit SBO terms in the resulting Association.
   * @return An Association object representing the gene product association, which could be a LogicalOperator or a direct GeneProductRef.
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
    return createGPR(ast.toString(), reactionId, model);
  }


  /**
   * Creates a GeneProductRef instance for a given identifier within a specific reaction context in the model.
   * This method first checks if the identifier exists in the model, either with or without a "G_" prefix.
   * If the identifier does not exist, it attempts to create a new GeneProduct in the model.
   * If the identifier exists, it updates the existing GeneProduct's ID.
   * 
   * @param identifier The identifier for the gene product, which may or may not start with "G_".
   * @param reactionId The ID of the reaction associated with this gene product.
   * @param model The SBML model containing the reaction and potentially the gene product.
   * @return A GeneProductRef object linked to the gene product identified or created.
   */
  public static GeneProductRef createGPR(String identifier, String reactionId, Model model) {
    // Determine the SBML document level and version for creating new elements.
    int level = model.getLevel(), version = model.getVersion();
    GeneProductRef gpr = new GeneProductRef(level, version);
    
    // Normalize the identifier to include "G_" prefix if missing.
    String oldId = identifier.startsWith("G_") ? identifier : "G_" + identifier;
    boolean containsOldId = !model.containsUniqueNamedSBase(oldId);
    
    // Attempt to create or find the GeneProduct using a standardized identifier.
    BiGGId.createGeneId(identifier).map(BiGGId::toBiGGId).ifPresent(id -> {
      if (!model.containsUniqueNamedSBase(id)) {
        GeneProduct gp;
        // Check if the old ID exists, if so, retrieve the GeneProduct, otherwise use the new ID.
        if (containsOldId) {
          gp = (GeneProduct) model.findUniqueNamedSBase(oldId);
        } else {
          gp = (GeneProduct) model.findUniqueNamedSBase(id);
        }
        // If the GeneProduct does not exist, create a new one and log a warning.
        if (gp == null) {
          logger.warning(format(MESSAGES.getString("CREATE_MISSING_GPR"), id, reactionId));
          FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
          gp = fbcPlug.createGeneProduct(id);
          gp.setLabel(id);
        } else {
          // If the GeneProduct exists, update its ID and log the update.
          logger.info(format(MESSAGES.getString("UPDATE_GP_ID"), gp.getId(), id));
          gp.setId(id);
        }
      }
      // Set the GeneProduct reference in the GeneProductRef.
      gpr.setGeneProduct(id);
    });
    return gpr;
  }


  /**
   * Parses the Gene Product Representation (GPR) for a given reaction and updates the reaction's gene product association.
   * If the reaction does not have an existing gene product association, a new one is created and set.
   * If an association already exists and it is not equivalent to the provided association, the associations are merged.
   *
   * @param r The reaction for which the GPR is being parsed.
   * @param association The association to be parsed and potentially merged into the reaction's gene product association.
   * @param omitGenericTerms A boolean flag indicating whether generic terms should be omitted during the merging process.
   */
  private static void parseGPR(Reaction r, Association association, boolean omitGenericTerms) {
    FBCReactionPlugin plugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
    if (!plugin.isSetGeneProductAssociation()) {
      GeneProductAssociation gpa = new GeneProductAssociation(r.getLevel(), r.getVersion());
      gpa.setAssociation(association);
      plugin.setGeneProductAssociation(gpa);
    } else if (!areEqual(association, plugin.getGeneProductAssociation().getAssociation())) {
      mergeAssociation(r, association, plugin, omitGenericTerms);
    }
  }


  private static boolean areEqual(Association gpa1, Association gpa2) {
    if (gpa1.isLeaf() && gpa2.isLeaf()) {
      return true;
    } else if (gpa1 instanceof Or && gpa2 instanceof Or) {
      if (((Or) gpa1).getNumChildren() != ((Or) gpa2).getNumChildren()) {
        return false;
      } else {
        boolean childrenEqual = true;
        for (int i = 0; i < ((Or) gpa1).getNumChildren(); i++) {
          childrenEqual &= areEqual((Association) gpa1.getChildAt(i), (Association) gpa2.getChildAt(i));
        }
        return childrenEqual;
      }
    } else if (gpa1 instanceof And && gpa2 instanceof And) {
      if (((And) gpa1).getNumChildren() != ((And) gpa2).getNumChildren()) {
        return false;
      } else {
        boolean childrenEqual = true;
        for (int i = 0; i < ((And) gpa1).getNumChildren(); i++) {
          childrenEqual &= areEqual((Association) gpa1.getChildAt(i), (Association) gpa2.getChildAt(i));
        }
        return childrenEqual;
      }
    }
    return false;
  }


  /**
   * Converts an Association object into a human-readable string representation.
   * This method handles different types of associations including GeneProductRef, And, and Or.
   * For And and Or associations, it recursively calls itself to handle nested associations.
   *
   * @param association The Association object to be converted into a string.
   * @return A string representation of the Association object.
   */
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


  /**
   * Merges a new association into an existing gene product association for a reaction.
   * This method handles the merging of associations by considering different types of associations (AND, OR, GeneProductRef).
   * It ensures that duplicate gene products are not added and maintains the logical structure of the association.
   *
   * @param r The reaction for which the gene product association is being merged.
   * @param association The new association to merge into the existing gene product association.
   * @param plugin The FBCReactionPlugin instance associated with the reaction.
   * @param omitGenericTerms Flag indicating whether to omit generic terms in the association.
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
      Set<String> gprs = new HashSet<>();
      for (int i = 0; i < or.getChildCount(); i++) {
        Association current = (Association) or.getChildAt(i);
        if (current instanceof GeneProductRef) {
          String geneProduct = ((GeneProductRef) current).getGeneProduct();
          if (gprs.contains(geneProduct)) {
            if (!or.removeAssociation(current)) {
              logger.warning(format("Failed to unset duplicate GeneProductReference '{0}' for reaction '{1}'",
                geneProduct, r.getId()));
            }
          } else {
            gprs.add(geneProduct);
          }
        }
      }
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
