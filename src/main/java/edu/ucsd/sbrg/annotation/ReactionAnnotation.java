package edu.ucsd.sbrg.annotation;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.polishing.PolishingUtils;
import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.db.QueryOnce;
import edu.ucsd.sbrg.miriam.Registry;
import edu.ucsd.sbrg.util.GPRParser;
import edu.ucsd.sbrg.util.SBMLUtils;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class provides functionality to annotate a reaction in an SBML model using BiGG database identifiers.
 * It extends the {@link CVTermAnnotation} class, allowing it to manage controlled vocabulary (CV) terms
 * associated with the reaction. The class handles various aspects of reaction annotation including setting
 * the reaction's name, SBO term, and additional annotations. It also processes gene reaction rules and
 * subsystem information associated with the reaction.
 */
public class ReactionAnnotation extends CVTermAnnotation {

  /**
   * A {@link Logger} for this class.
   */
  static final transient Logger logger = Logger.getLogger(ReactionAnnotation.class.getName());
  /**
   * Localization support.
   */
  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");
  /**
   * Instance of reaction to annotate
   */
  private final Reaction reaction;
  private static boolean triggeredSubsystemWarning = false;
  private final Parameters parameters;


  public ReactionAnnotation(Reaction reaction, Parameters parameters) {
    super(parameters);
    this.reaction = reaction;
    this.parameters = parameters;
  }


  /**
   * Annotates a reaction by setting its name, SBO term, and additional annotations. It also processes gene reaction rules
   * and subsystem information associated with the reaction. This method retrieves a BiGG ID for the reaction, either from
   * the reaction's ID directly or through associated annotations. If a valid BiGG ID is found, it proceeds with the
   * annotation and parsing processes.
   */
  @Override
  public void annotate() {
    // Attempt to retrieve a BiGG ID for the reaction, either directly from the reaction ID or through associated annotations
    checkId().ifPresent(biggId -> {
      setName(biggId); // Set the reaction's name based on the BiGG ID
      setSBOTerm(biggId); // Assign the appropriate SBO term based on the BiGG ID
      addAnnotations(biggId); // Add additional annotations related to the BiGG ID
      parseGeneReactionRules(biggId); // Parse and process gene reaction rules associated with the BiGG ID
      parseSubsystems(biggId); // Convert subsystem information into corresponding groups based on the BiGG ID
    });
  }

  /**
   * This method checks if the ID of the reaction is a valid BiGG ID and attempts to retrieve a corresponding
   * BiGG ID based on existing annotations. It first checks if the reaction ID matches the expected BiGG ID format
   * and verifies its existence in the database. If the ID does not match or is not found, it then attempts to
   * extract a BiGG ID from the reaction's annotations. This involves parsing the CVTerms associated with the reaction,
   * extracting URLs, validating them, and then querying the BiGG database for corresponding reaction IDs that match
   * the reaction's compartment.
   *
   * @return An {@link Optional} containing the BiGG ID if found or created successfully, otherwise {@link Optional#empty()}
   */
  @Override
  public Optional<BiGGId> checkId() {
    String id = reaction.getId();
    // Check if the reaction ID matches the expected BiGG ID format and exists in the database
    boolean isBiGGid = id.matches("^(R_)?([a-zA-Z][a-zA-Z0-9_]+)(?:_([a-z][a-z0-9]?))?(?:_([A-Z][A-Z0-9]?))?$")
            && QueryOnce.isReaction(id);
    if (!isBiGGid) {
      // Extract BiGG IDs from annotations if the direct ID check fails
      Set<String> ids = reaction.getAnnotation().getListOfCVTerms()
              .stream()
              .filter(cvTerm -> cvTerm.getQualifier() == Qualifier.BQB_IS)
              .flatMap(term -> term.getResources().stream())
              .map(Registry::checkResourceUrl)
              .flatMap(Optional::stream)
              .map(Registry::getPartsFromIdentifiersURI)
              .map(parts -> {
                String prefix = parts.get(0);
                String synonymId = parts.get(1);
                return BiGGDB.getBiggIdsForReactionForeignId(prefix, synonymId);
              })
              .flatMap(Collection::stream)
              .filter(this::matchingCompartments)
              .map(fr -> fr.reactionId)
              .collect(Collectors.toSet());
      // Select the first valid ID from the set, if available
      id = ids.stream()
              .findFirst()
              .orElse(id);
    }
    return BiGGId.createReactionId(id);
  }

  /**
   * Determines if the reaction's compartment matches the compartment information of a foreign reaction from the BiGG database.
   * 
   * This method checks various conditions to ensure that the compartments are correctly matched:
   * 1. If the reaction does not have a compartment set and the foreign reaction also lacks compartment details, it returns true.
   * 2. If the reaction does not have a compartment set but the foreign reaction does, it returns false.
   * 3. If the reaction has a compartment set, it checks if the compartment ID matches the foreign reaction's compartment ID.
   * 4. If the reaction has a named compartment instance set, it checks if the name matches the foreign reaction's compartment name.
   * 
   * @param foreignReaction The foreign reaction object containing compartment details to compare against the reaction.
   * @return true if the compartments match according to the conditions above, false otherwise.
   */
  private boolean matchingCompartments(BiGGDB.ForeignReaction foreignReaction) {
    if (!reaction.isSetCompartment()
            && null == foreignReaction.compartmentId
            && null == foreignReaction.compartmentName) {
      return true;
    } else if (!reaction.isSetCompartment()
            && (null != foreignReaction.compartmentId
            || null != foreignReaction.compartmentName)) {
      return false;
    } else if (reaction.isSetCompartment()) {
      return reaction.getCompartment()
              .equals(foreignReaction.compartmentId);
    } else if (reaction.isSetCompartmentInstance()
            && reaction.getCompartmentInstance().isSetName()) {
      return reaction.getCompartmentInstance().getName()
              .equals(foreignReaction.compartmentName);
    } else
      return false;
  }

  /**
   * Sets the name of the reaction based on the provided BiGGId. It retrieves the reaction name using the abbreviation
   * from the BiGGId, polishes the name, and updates the reaction's name if the new name is different from the current name.
   * 
   * @param biggId The BiGGId object containing the abbreviation used to fetch and potentially update the reaction's name.
   */
  public void setName(BiGGId biggId) {
    String abbreviation = biggId.getAbbreviation();
    BiGGDB.getReactionName(abbreviation)
            .filter(name -> !name.equals(reaction.getName()))
            .map(PolishingUtils::polishName)
            .ifPresent(reaction::setName);
  }

  /**
   * Sets the SBO term for a reaction based on the given BiGGId.
   * If the reaction does not already have an SBO term set, it determines the appropriate SBO term
   * based on whether the reaction is a pseudoreaction or a generic process. Pseudoreactions are assigned
   * an SBO term of 631, while generic processes are assigned an SBO term of 375 unless the configuration
   * specifies to omit generic terms.
   *
   * @param biggId The BiGGId object containing the abbreviation used to check if the reaction is a pseudoreaction.
   */
  public void setSBOTerm(BiGGId biggId) {
    String abbreviation = biggId.getAbbreviation();
    if (!reaction.isSetSBOTerm()) {
      if (BiGGDB.isPseudoreaction(abbreviation)) {
        reaction.setSBOTerm(631);
      } else if (!parameters.omitGenericTerms()) {
        reaction.setSBOTerm(375); // generic process
      }
    }
  }


  /**
   * This method delegates the task of adding annotations to a reaction based on the provided {@link BiGGId}.
   * It specifically focuses on updating MIRIAM URIs from http to https, merging duplicate annotations, and
   * ensuring that the reaction is annotated with the correct identifiers from the BiGG database.
   *
   * @param biggId The {@link BiGGId} associated with the reaction, used to fetch and apply annotations.
   */
  @Override
  public void addAnnotations(BiGGId biggId) {
    addAnnotations(reaction, biggId);
  }

  /**
   * Parses gene reaction rules for a given reaction based on the BiGG database identifier.
   * This method retrieves gene reaction rules associated with the reaction's abbreviation
   * from the BiGG database and applies gene-protein-reaction (GPR) parsing to the reaction.
   * It considers whether generic terms should be omitted based on the current parameters.
   *
   * @param biggId The BiGG database identifier for the reaction, used to fetch and parse gene reaction rules.
   */
  public void parseGeneReactionRules(BiGGId biggId) {
    String abbreviation = biggId.getAbbreviation();
    List<String> geneReactionRules = BiGGDB.getGeneReactionRule(abbreviation, reaction.getModel().getId());
    for (String geneReactionRule : geneReactionRules) {
      GPRParser.parseGPR(reaction, geneReactionRule, parameters.omitGenericTerms());
    }
  }


  /**
   * Retrieves subsystem information from the BiGG Knowledgebase and converts it into corresponding groups using the
   * {@link GroupsModelPlugin}. It then links the reaction to the appropriate group based on the subsystem information.
   * If the model is not from BiGG, it logs a warning and uses a different method to fetch subsystems.
   * It also ensures that subsystems are unique by converting them to lowercase and removing duplicates.
   * If multiple subsystems are found for a non-BiGG model, the method returns early to avoid ambiguity.
   * Each subsystem is then either fetched from a cache or a new group is created and added to the model.
   * Finally, the reaction is linked to the group.
   *
   * @param biggId the {@link BiGGId} associated with the reaction, used to fetch subsystem information
   */
  private void parseSubsystems(BiGGId biggId) {
    Model model = reaction.getModel();
    boolean isBiGGModel = QueryOnce.isModel(model.getId());
    List<String> subsystems;
    if (isBiGGModel) {
      subsystems = BiGGDB.getSubsystems(model.getId(), biggId.getAbbreviation());
    } else {
      if (!triggeredSubsystemWarning) {
        triggeredSubsystemWarning = true;
        logger.warning(MESSAGES.getString("SUBSYSTEM_MODEL_NOT_BIGG"));
      }
      subsystems = BiGGDB.getSubsystemsForReaction(biggId.getAbbreviation());
    }
    if (subsystems.size() < 1) {
      return;
    } else {
      // filter out duplicates only differing in case - relevant for #getSubsystemsForReaction results
      subsystems = subsystems.stream().map(String::toLowerCase).distinct().collect(Collectors.toList());
      // Code already allows for multiple results from one query. If we have no BiGG model id, this might lead to
      // ambiguous, incorrect results
      if (!isBiGGModel && subsystems.size() > 1) {
        return;
      }
    }
    String groupKey = "GROUP_FOR_NAME";
    if (model.getUserObject(groupKey) == null) {
      model.putUserObject(groupKey, new HashMap<String, Group>());
    }
    @SuppressWarnings("unchecked")
    Map<String, Group> groupForName = (Map<String, Group>) model.getUserObject(groupKey);
    for (String subsystem : subsystems) {
      Group group;
      if (groupForName.containsKey(subsystem)) {
        group = groupForName.get(subsystem);
      } else {
        GroupsModelPlugin groupsModelPlugin = (GroupsModelPlugin) model.getPlugin(GroupsConstants.shortLabel);
        group = groupsModelPlugin.createGroup("g" + (groupsModelPlugin.getGroupCount() + 1));
        group.setName(subsystem);
        group.setKind(Group.Kind.partonomy);
        group.setSBOTerm(633); // subsystem
        groupForName.put(subsystem, group);
      }
      SBMLUtils.createSubsystemLink(reaction, group.createMember());
    }
  }
}
