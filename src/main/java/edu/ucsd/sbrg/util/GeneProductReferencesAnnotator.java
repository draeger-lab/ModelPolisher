package edu.ucsd.sbrg.util;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.*;

import javax.swing.tree.TreeNode;
import java.util.HashMap;
import java.util.Map;

public class GeneProductReferencesAnnotator {

    /**
     * A static map that holds references to all gene products within a model, facilitating quick updates.
     */
    private Map<String, GeneProductRef> geneProductReferences = new HashMap<>();


    /**
     * Clears the geneProductReferences map, removing all entries.
     */
    public void clearGPRMap() {
        geneProductReferences = new HashMap<>();
    }



    /**
     * Initializes the geneProductReferences map by traversing through all reactions and their associated gene products.
     * It handles both direct gene product references and nested logical operators that might contain gene product references.
     *
     * @param reactions The list of reactions to scan for gene product associations.
     */
    private void init(ListOf<Reaction> reactions) {
        for (Reaction r : reactions) {
            for (int childIdx = 0; childIdx < r.getChildCount(); childIdx++) {
                TreeNode child = r.getChildAt(childIdx);
                if (child instanceof GeneProductAssociation) {
                    Association association = ((GeneProductAssociation) child).getAssociation();
                    if (association instanceof GeneProductRef gpr) {
                        geneProductReferences.put(gpr.getGeneProduct(), gpr);
                    } else if (association instanceof LogicalOperator) {
                        updateFromNestedAssociations(association);
                    }
                }
            }
        }
    }


    /**
     * Updates the reference of a gene product in the geneProductReferences map using the gene product's ID.
     * If the gene product ID starts with "G_", it strips this prefix before updating.
     * If the map is initially empty, it initializes the map with the current model's reactions.
     *
     * @param gp The GeneProduct whose reference needs to be updated.
     */
    public void update(GeneProduct gp) {
        if (geneProductReferences.isEmpty()) {
            init(gp.getModel().getListOfReactions());
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
     * Recursively processes nested associations to map gene products to their references.
     * This method traverses through each child of the given association. If the child is a
     * LogicalOperator, it recursively processes it. If the child is a GeneProductRef, it
     * adds it to the geneProductReferences map.
     *
     * @param association The association to process, which can contain nested associations.
     */
    private void updateFromNestedAssociations(Association association) {
        for (int idx = 0; idx < association.getChildCount(); idx++) {
            TreeNode child = association.getChildAt(idx);
            if (child instanceof LogicalOperator) {
                updateFromNestedAssociations((Association) child);
            } else {
                // This child is assumed to be a GeneProductRef
                GeneProductRef gpr = (GeneProductRef) child;
                geneProductReferences.put(gpr.getGeneProduct(), gpr);
            }
        }
    }
}
