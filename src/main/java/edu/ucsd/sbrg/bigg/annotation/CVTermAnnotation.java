package edu.ucsd.sbrg.bigg.annotation;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.bigg.BiGGId;
import edu.ucsd.sbrg.bigg.Parameters;
import edu.ucsd.sbrg.db.AnnotateDB;
import edu.ucsd.sbrg.db.BiGGDB;
import edu.ucsd.sbrg.db.QueryOnce;
import edu.ucsd.sbrg.miriam.Registry;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.BIGG_METABOLITE;
import static edu.ucsd.sbrg.db.AnnotateDBContract.Constants.BIGG_REACTION;
import static java.text.MessageFormat.format;

/**
 * Abstract class providing a framework for annotating SBML elements with Controlled Vocabulary (CV) Terms.
 * This class defines the basic structure and operations for adding annotations to SBML elements based on BiGG IDs.
 * It includes methods to check the validity of BiGG IDs, add annotations to SBML elements, and specifically handle
 * annotations for Species and Reactions using data from BiGG and other databases.
 */
public abstract class CVTermAnnotation {

  /**
   * Localization support.
   */
  private static final transient ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

  /**
   * Abstract method to annotate an SBML element. Implementations should define specific annotation logic.
   */
  abstract void annotate();

  /**
   * Abstract method to check the validity of a BiGG ID. Implementations should return an Optional containing
   * the BiGG ID if it is valid, or an empty Optional if not.
   *
   * @return Optional containing the valid BiGG ID or empty if the ID is invalid.
   */
  abstract Optional<BiGGId> checkId();

  /**
   * Abstract method to add annotations to an SBML element using a BiGG ID. Implementations should define how
   * annotations are added based on the specific type of SBML element and the source of the annotations.
   *
   * @param biggId The BiGGId used for fetching annotations.
   */
  abstract void addAnnotations(BiGGId biggId);

  /**
   * Adds annotations to an SBML node (either a Species or a Reaction) using a given BiGGId.
   * This method first checks if the node is an instance of Species or Reaction and throws an IllegalArgumentException if not.
   * It then removes any existing CVTerm with the qualifier BQB_IS, prepares a new CVTerm if none existed,
   * and collects annotations from various sources (BiGG database, AnnotateDB) based on whether the node is a Species or Reaction.
   * These annotations are filtered to remove any that already exist on the node, sorted, and then added to the node.
   * Finally, it ensures that the node has a metaId set if it has any CVTerms.
   *
   * @param node The SBML node to annotate, which should be either a Species or a Reaction.
   * @param biggId The BiGGId used for fetching annotations.
   * @throws IllegalArgumentException If the node is neither a Species nor a Reaction.
   */
  void addAnnotations(SBase node, BiGGId biggId) throws IllegalArgumentException {
    if (!(node instanceof Species) && !(node instanceof Reaction)) {
      throw new IllegalArgumentException(format(MESSAGES.getString("ANNOTATION_WRONG_SBASE"), node.getClass().getName()));
    }
    // Fetch annotations already present on the SBase
    CVTerm cvTerm = null;
    for (CVTerm term : node.getAnnotation().getListOfCVTerms()) {
      if (term.getQualifier() == Qualifier.BQB_IS) {
        cvTerm = term;
        node.removeCVTerm(term);
        break;
      }
    }
    if (cvTerm == null) {
      cvTerm = new CVTerm(Qualifier.BQB_IS);
    }
    Set<String> annotations = new HashSet<>();
    Parameters parameters = Parameters.get();
    boolean isReaction = node instanceof Reaction;
    if (isReaction) {
      boolean isBiGGReaction = QueryOnce.isReaction(biggId.getAbbreviation());
      // using BiGG Database
      if (isBiGGReaction) {
        annotations.add(Registry.createURI("bigg.reaction", biggId));
      }
      Set<String> biggAnnotations = BiGGDB.getResources(biggId, parameters.includeAnyURI(), true);
      annotations.addAll(biggAnnotations);
      // using AnnotateDB
      if (parameters.addADBAnnotations() && AnnotateDB.inUse() && isBiGGReaction) {
        Set<String> adbAnnotations = AnnotateDB.getAnnotations(BIGG_REACTION, biggId.toBiGGId());
        annotations.addAll(adbAnnotations);
      }
    } else {
      boolean isBiGGMetabolite = QueryOnce.isMetabolite(biggId.getAbbreviation());
      // using BiGG Database
      if (isBiGGMetabolite) {
        annotations.add(Registry.createURI("bigg.metabolite", biggId));
      }
      Set<String> biggAnnotations = BiGGDB.getResources(biggId, parameters.includeAnyURI(), false);
      annotations.addAll(biggAnnotations);
      // using AnnotateDB
      if (parameters.addADBAnnotations() && AnnotateDB.inUse() && isBiGGMetabolite) {
        Set<String> adb_annotations = AnnotateDB.getAnnotations(BIGG_METABOLITE, biggId.toBiGGId());
        annotations.addAll(adb_annotations);
      }
    }
    // don't add resources that are already present
    Set<String> existingAnnotations =
      cvTerm.getResources().stream()
            .map(resource -> resource.replaceAll("http://identifiers.org", "https://identifiers.org"))
            .collect(Collectors.toSet());
    annotations.removeAll(existingAnnotations);
    // adding annotations to cvTerm
    List<String> sortedAnnotations = new ArrayList<>(annotations);
    Collections.sort(sortedAnnotations);
    for (String annotation : sortedAnnotations) {
      cvTerm.addResource(annotation);
    }
    if (cvTerm.getResourceCount() > 0) {
      node.addCVTerm(cvTerm);
    }
    if ((node.getCVTermCount() > 0) && !node.isSetMetaId()) {
      node.setMetaId(node.getId());
    }
  }
}
