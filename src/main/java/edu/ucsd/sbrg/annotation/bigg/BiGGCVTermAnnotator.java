package edu.ucsd.sbrg.annotation.bigg;

import edu.ucsd.sbrg.db.bigg.BiGGId;
import edu.ucsd.sbrg.db.bigg.BiGGDB;
import edu.ucsd.sbrg.parameters.BiGGAnnotationParameters;
import edu.ucsd.sbrg.resolver.Registry;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import org.sbml.jsbml.*;

import java.sql.SQLException;
import java.util.List;

/**
 * Abstract class providing a framework for annotating SBML elements with Controlled Vocabulary (CV) Terms.
 * This class defines the basic structure and operations for adding annotations to SBML elements based on BiGG IDs.
 * It includes methods to check the validity of BiGG IDs, add annotations to SBML elements, and specifically handle
 * annotations for Species and Reactions using data from BiGG and other databases.
 */
public abstract class BiGGCVTermAnnotator<T extends SBase> extends AbstractBiGGAnnotator {

  public BiGGCVTermAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, Registry registry) {
    super(bigg, parameters, registry);
  }

  public BiGGCVTermAnnotator(BiGGDB bigg, BiGGAnnotationParameters parameters, Registry registry, List<ProgressObserver> observers) {
    super(bigg, parameters, registry, observers);
  }


  /**
   * Abstract method to check the validity of a BiGG ID. Implementations should return an Optional containing
   * the BiGG ID if it is valid, or an empty Optional if not.
   *
   * @return Optional containing the valid BiGG ID or empty if the ID is invalid.
   */
  protected abstract BiGGId findBiGGId(T element) throws SQLException;

//  /**
//   * Adds annotations to an SBML node (either a Species or a Reaction) using a given BiGGId.
//   * This method first checks if the node is an instance of Species or Reaction and throws an IllegalArgumentException if not.
//   * It then removes any existing CVTerm with the qualifier BQB_IS, prepares a new CVTerm if none existed,
//   * and collects annotations from various sources (BiGG database, AnnotateDB) based on whether the node is a Species or Reaction.
//   * These annotations are filtered to remove any that already exist on the node, sorted, and then added to the node.
//   * Finally, it ensures that the node has a metaId set if it has any CVTerms.
//   *
//   * @param node The SBML node to annotate, which should be either a Species or a Reaction.
//   * @param biggId The BiGGId used for fetching annotations.
//   * @throws IllegalArgumentException If the node is neither a Species nor a Reaction.
//   */
//  void addAnnotations(T node, BiGGId biggId) throws IllegalArgumentException {
//
//    // TODO: ???
//    CVTerm cvTerm = null;
//    for (CVTerm term : node.getAnnotation().getListOfCVTerms()) {
//      if (term.getQualifier() == Qualifier.BQB_IS) {
//        cvTerm = term;
//        node.removeCVTerm(term);
//        break;
//      }
//    }
//    if (cvTerm == null) {
//      cvTerm = new CVTerm(Qualifier.BQB_IS);
//    }
//
//    Set<String> annotations = new HashSet<>();
//
//      if (node instanceof Reaction) {
//      boolean isBiGGReaction = MemorizedQuery.isReaction(biggId.getAbbreviation());
//      // using BiGG Database
//      if (isBiGGReaction) {
//        annotations.add(new IdentifiersOrgURI("bigg.reaction", biggId).getURI());
//
//        Set<String> biggAnnotations = bigg.getResources(biggId, parameters.includeAnyURI(), true)
//                .stream()
//                .map(IdentifiersOrgURI::getURI)
//                .collect(Collectors.toSet());
//        annotations.addAll(biggAnnotations);
//
//        // using AnnotateDB
//        if (MemorizedQuery.isReaction(biggId.getAbbreviation()) && adb != null) {
//          Set<String> adbAnnotations = AnnotateDB.getAnnotations(BIGG_REACTION, biggId.toBiGGId());
//          annotations.addAll(adbAnnotations);
//        }
//      }
//
//
//    } else {
//      boolean isBiGGMetabolite = MemorizedQuery.isMetabolite(biggId.getAbbreviation());
//      // using BiGG Database
//      if (isBiGGMetabolite) {
//        annotations.add(new IdentifiersOrgURI("bigg.metabolite", biggId).getURI());
//
//        Set<String> biggAnnotations = bigg.getResources(biggId, parameters.includeAnyURI(), false)
//                .stream().map(IdentifiersOrgURI::getURI).collect(Collectors.toSet());
//        annotations.addAll(biggAnnotations);
//
//        // using AnnotateDB
//        if (adb != null) {
//          Set<String> adb_annotations = AnnotateDB.getAnnotations(BIGG_METABOLITE, biggId.toBiGGId());
//          annotations.addAll(adb_annotations);
//        }
//      }
//
//    }
//    // don't add resources that are already present
//    Set<String> existingAnnotations =
//      cvTerm.getResources().stream()
//            .map(resource -> resource.replaceAll("http://identifiers.org", "https://identifiers.org"))
//            .collect(Collectors.toSet());
//    annotations.removeAll(existingAnnotations);
//    // adding annotations to cvTerm
//    List<String> sortedAnnotations = new ArrayList<>(annotations);
//    Collections.sort(sortedAnnotations);
//    for (String annotation : sortedAnnotations) {
//      cvTerm.addResource(annotation);
//    }
//    if (cvTerm.getResourceCount() > 0) {
//      node.addCVTerm(cvTerm);
//    }
//    if ((node.getCVTermCount() > 0) && !node.isSetMetaId()) {
//      node.setMetaId(node.getId());
//    }
//  }
}
