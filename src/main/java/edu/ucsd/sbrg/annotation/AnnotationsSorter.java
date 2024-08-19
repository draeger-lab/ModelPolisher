package edu.ucsd.sbrg.annotation;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.logging.BundleNames;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.tree.TreeNode;
import java.util.*;

import static java.text.MessageFormat.format;

public class AnnotationsSorter {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationsSorter.class);
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle(BundleNames.ANNOTATION_MESSAGES);


    /**
     * Recursively goes through all annotations in the given {@link SBase} and
     * alphabetically sort annotations after grouping them by {@link org.sbml.jsbml.CVTerm.Qualifier}.
     *
     * @param sbase:
     *        {@link SBase} to start the merging process at, corresponding to an instance of {@link SBMLDocument} here,
     *        though also used to pass current {@link SBase} during recursion
     */
    public void groupAndSortAnnotations(SBase sbase) {
        if (sbase.isSetAnnotation()) {
            SortedMap<CVTerm.Qualifier, SortedSet<String>> miriam = new TreeMap<>();
            boolean doMerge = hashMIRIAMuris(sbase, miriam);
            if (doMerge) {
                sbase.getAnnotation().unsetCVTerms();
                for (Map.Entry<CVTerm.Qualifier, SortedSet<String>> entry : miriam.entrySet()) {
                    logger.debug(format(MESSAGES.getString("MERGING_MIRIAM_RESOURCES"), entry.getKey(),
                            sbase.getClass().getSimpleName(), sbase.getId()));
                    sbase.addCVTerm(new CVTerm(entry.getKey(), entry.getValue().toArray(new String[0])));
                }
            }
        }
        for (int i = 0; i < sbase.getChildCount(); i++) {
            TreeNode node = sbase.getChildAt(i);
            if (node instanceof SBase) {
                groupAndSortAnnotations((SBase) node);
            }
        }
    }

    /**
     * Evaluates and merges CVTerm annotations for a given SBase element. This method checks each CVTerm associated with
     * the SBase and determines if there are multiple CVTerms with the same Qualifier that need merging. It also corrects
     * invalid qualifiers based on the type of SBase (Model or other biological elements).
     *
     * @param sbase The SBase element whose annotations are to be evaluated and potentially merged.
     * @param miriam A sorted map that groups CVTerm resources by their qualifiers.
     * @return true if there are CVTerms with the same qualifier that need to be merged, false otherwise.
     */
    private boolean hashMIRIAMuris(SBase sbase, SortedMap<CVTerm.Qualifier, SortedSet<String>> miriam) {
        boolean doMerge = false;
        for (int i = 0; i < sbase.getCVTermCount(); i++) {
            CVTerm term = sbase.getCVTerm(i);
            CVTerm.Qualifier qualifier = term.getQualifier();
            if (miriam.containsKey(qualifier)) {
                doMerge = true;
            } else {
                if (sbase instanceof Model) {
                    if (!qualifier.isModelQualifier()) {
                        logger.debug(format(MESSAGES.getString("CORRECTING_INVALID_QUALIFIERS"),
                                qualifier.getElementNameEquivalent(), sbase.getId()));
                        qualifier = CVTerm.Qualifier.getModelQualifierFor(qualifier.getElementNameEquivalent());
                    }
                } else if (!qualifier.isBiologicalQualifier()) {
                    logger.debug(format(MESSAGES.getString("CORRECTING_INVALID_MODEL_QUALIFIER"),
                            qualifier.getElementNameEquivalent(), sbase.getClass().getSimpleName(), sbase.getId()));
                    qualifier = CVTerm.Qualifier.getBiologicalQualifierFor(qualifier.getElementNameEquivalent());
                }
                miriam.put(qualifier, new TreeSet<>());
            }
            miriam.get(qualifier).addAll(term.getResources());
        }
        return doMerge;
    }

}
