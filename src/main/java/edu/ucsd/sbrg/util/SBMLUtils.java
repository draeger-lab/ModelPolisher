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

/**
 * A collection of helpful functions for dealing with SBML data structures.
 * 
 * @author Andreas Dr&auml;ger
 */
public class SBMLUtils {

  /**
   * Key to link from {@link Reaction} directly to {@link Member}s referencing
   * that reaction.
   */
  public static final String SUBSYSTEM_LINK = "SUBSYSTEM_LINK";


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
