package de.uni_halle.informatik.biodata.mp.io;

import de.zbit.util.ResourceManager;
import de.uni_halle.informatik.biodata.mp.logging.BundleNames;
import de.uni_halle.informatik.biodata.mp.util.ext.groups.GroupsUtils;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.ext.groups.Member;
import org.sbml.jsbml.util.TreeNodeChangeEvent;
import org.sbml.jsbml.util.TreeNodeChangeListener;
import org.sbml.jsbml.util.TreeNodeRemovedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.tree.TreeNode;
import java.beans.PropertyChangeEvent;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;


/**
 * The {@code UpdateListener} class implements the {@link TreeNodeChangeListener} to monitor and respond to changes
 * within an SBML model's structure. This class specifically handles updates to
 * identifiers (IDs) of model elements like reactions and gene products, ensuring that all references remain consistent
 * across the model. It also manages the addition of new nodes to the model, particularly focusing on gene product
 * references, and maintains a mapping from gene identifiers to their associated gene product references.
 * <p>
 * The {@link TreeNodeChangeListener} base class provides the interface for receiving notifications when changes occur
 * to any node within a tree structure, which in the context of SBML, corresponds to elements within the model's
 * hierarchical structure.
 * 
 * @author Andreas Dr&auml;ger
 */
public class UpdateListener implements TreeNodeChangeListener {

  private static final Logger logger = LoggerFactory.getLogger(UpdateListener.class);

  /**
   * Bundle for ModelPolisher logger messages
   */
  private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.IO_MESSAGES  );
  /**
   * A map that maintains associations between gene identifiers and sets of {@link GeneProductRef} objects.
   * This map is used to track which gene products are associated with specific gene identifiers throughout the model.
   */
  private final Map<String, Set<GeneProductRef>> geneIdToAssociation;

  /**
   * Constructs an {@code UpdateListener} instance and initializes the {@code geneIdToAssociation} map.
   */
  public UpdateListener() {
    geneIdToAssociation = new HashMap<>();
  }


  /**
   * Responds to property change events, specifically focusing on changes to the ID property of tree nodes.
   * This method handles the update of IDs within the model, ensuring that all references to the old ID
   * are updated to the new ID across various components such as reactions and gene products.
   *
   * @param evt The property change event that contains information about the old and new values of the property.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    // Check if the property change is related to the ID of a node.
    if (evt.getPropertyName().equals(TreeNodeChangeEvent.id)) {
      String oldId = (String) evt.getOldValue();
      // Proceed only if there is an actual change in the ID.
      if (oldId != null) {
        // There is only a need to do some further change if the id is updated
        // to a new id.
        String newId = (String) evt.getNewValue();
        NamedSBase nsb = (NamedSBase) evt.getSource();
        // Handle the ID change for reactions.
        if (nsb instanceof Reaction r) {
            Model model = r.getModel();
          FBCModelPlugin fbcModelPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
          // Update reaction references in the FBC model plugin.
          updateReactionRef(oldId, newId, fbcModelPlug);
          // Update subsystem references if any.
          Set<Member> subsystems = (Set<Member>) r.getUserObject(GroupsUtils.SUBSYSTEM_LINK);
          if (subsystems != null) {
            for (Member m : subsystems) {
              m.setIdRef(newId);
            }
          }
        } 
        // Handle the ID change for gene products.
        else if (nsb instanceof GeneProduct) {
          Set<GeneProductRef> geneRefs = geneIdToAssociation.remove(oldId);
          if (geneRefs != null) {
            for (GeneProductRef ref : geneRefs) {
              ref.setGeneProduct(newId);
            }
            geneIdToAssociation.put(newId, geneRefs);
          }
        } 
        // Log a severe message if the ID change cannot be handled.
        else {
          logger.debug(
            MessageFormat.format(MESSAGES.getString("ID_CHANGE_WARNING"), nsb.getElementName(), oldId, newId));
        }
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
  private void updateReactionRef(String oldId, String newId, FBCModelPlugin fbcModelPlug) {
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
   * Handles the event when a new node is added to the TreeNode structure.
   * Specifically, when a GeneProductRef node is added, this method updates the
   * geneIdToAssociation map to include this new association. It ensures that each
   * gene product ID is mapped to a set of its associated GeneProductRefs.
   *
   * @param node The TreeNode that has been added. Expected to be an instance of GeneProductRef.
   */
  @Override
  public void nodeAdded(TreeNode node) {
    // Memorize link from GeneProduct to Associations when this association is
    // being added.
    if (node instanceof GeneProductRef gpr) {
        // Retrieve or create a set of GeneProductRefs associated with the gene product ID.
        Set<GeneProductRef> geneRefs = geneIdToAssociation.computeIfAbsent(gpr.getGeneProduct(), k -> new HashSet<>());
        geneRefs.add(gpr);
      // The following commented code would link the gene product instance directly to its associations.
      // GeneProduct gene = gpr.getGeneProductInstance();
      // if (gene != null) {
      //   gene.putUserObject("ASSOCIATION_LINK", geneRefs);
      // }
    }
    // Log the addition of the node.
    logger.debug(node.toString());
  }


  /*
   * (non-Javadoc)
   * @see
   * org.sbml.jsbml.util.TreeNodeChangeListener#nodeRemoved(org.sbml.jsbml.util.
   * TreeNodeRemovedEvent)
   */
  @Override
  public void nodeRemoved(TreeNodeRemovedEvent event) {
    logger.debug(event.toString());
  }
}
