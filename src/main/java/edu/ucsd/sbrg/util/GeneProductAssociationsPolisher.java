package edu.ucsd.sbrg.util;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.bigg.BiGGId;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.xml.XMLNode;

import java.util.*;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

public class GeneProductAssociationsPolisher {

    /**
     * A {@link Logger} for this class.
     */
    private static final Logger logger = Logger.getLogger(GeneProductAssociationsPolisher.class.getName());
    /**
     * Bundle for ModelPolisher logger messages
     */
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");


    /**
     * Mapping holding geneAssociations from model annotations
     */
    private Map<String, XMLNode> oldGeneAssociations;




    /**
     * resets Map containing geneAssociation XMLNodes, as it is only valid for one model
     */
    public void clearAssociationMap() {
        if (oldGeneAssociations != null) {
            oldGeneAssociations = null;
        }
    }


    /**
     * Converts gene product associations from a given reaction to the FBC v2 format.
     * This method processes the non-RDF annotations of the reaction's model to update or create
     * gene product associations according to the FBC v2 specification.
     *
     * @param reaction The reaction whose gene product associations are to be converted.
     * @param omitGenericTerms A boolean flag indicating whether to omit generic terms (SBO terms) in the association.
     */
    public void convertAssociationsToFBCV2(Reaction reaction, boolean omitGenericTerms) {
        Model model = reaction.getModel();
        Annotation annotation = model.getAnnotation();
        XMLNode node = annotation.getNonRDFannotation();
        if (node == null) {
            return;
        }
        if (oldGeneAssociations == null) {
            oldGeneAssociations = new HashMap<>();
            for (int i = 0; i < node.getChildCount(); i++) {
                XMLNode current = node.getChild(i);
                if (current.getName().equals("geneAssociation")) {
                    String reactionId = current.getAttributes().getValue("reaction");
                    oldGeneAssociations.put(reactionId, node.getChild(i));
                    node.removeChild(i);
                }
            }
        }
        String id = reaction.getId();
        XMLNode ga = oldGeneAssociations.getOrDefault(id, null);
        if (ga != null) {
            FBCReactionPlugin plugin = (FBCReactionPlugin) reaction.getPlugin(FBCConstants.shortLabel);
            GeneProductAssociation gpa = new GeneProductAssociation(reaction.getLevel(), reaction.getVersion());
            List<Association> associations = processAssociation(ga, model, omitGenericTerms);
            if (associations.size() == 1) {
                gpa.setAssociation(associations.get(0));
                plugin.setGeneProductAssociation(gpa);
            }
        }
    }

    /**
     * Processes an XMLNode representing a gene product association and converts it into a list of Association objects.
     * This method handles the logical operators "and" and "or", as well as gene product references.
     *
     * @param association The XMLNode representing the gene product association.
     * @param model The SBML model to which the association belongs.
     * @param omitGenericTerms A boolean flag indicating whether to omit generic terms (SBO terms) in the association.
     * @return A list of Association objects representing the processed gene product association.
     */
    private List<Association> processAssociation(XMLNode association, Model model, boolean omitGenericTerms) {
        int level = model.getLevel(), version = model.getVersion();
        List<Association> associations = new ArrayList<>();
        for (int i = 0; i < association.getChildCount(); i++) {
            XMLNode current = association.getChild(i);
            switch (current.getName()) {
                case "and":
                    And and = new And(level, version);
                    if (!omitGenericTerms) {
                        and.setSBOTerm(173); // AND
                    }
                    and.addAllAssociations(processAssociation(current, model, omitGenericTerms));
                    if (and.isSetListOfAssociations()) {
                        associations.add(and);
                    }
                    break;
                case "or":
                    Or or = new Or(level, version);
                    if (!omitGenericTerms) {
                        or.setSBOTerm(174); // OR
                    }
                    or.addAllAssociations(processAssociation(current, model, omitGenericTerms));
                    if (or.isSetListOfAssociations()) {
                        associations.add(or);
                    }
                    break;
                case "gene":
                    String geneReference = current.getAttributes().getValue("reference");
                    GeneProductRef gpr = new GeneProductRef(level, version);
                    BiGGId.createGeneId(geneReference).map(BiGGId::toBiGGId).ifPresent(id -> {
                        if (!model.containsUniqueNamedSBase(id)) {
                            GeneProduct gp = (GeneProduct) model.findUniqueNamedSBase(id);
                            if (gp == null) {
                                logger.warning(format("Creating missing gene product {0}", id));
                                FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
                                gp = fbcPlug.createGeneProduct(id);
                                gp.setLabel(id);
                            } else {
                                logger.info(format(MESSAGES.getString("UPDATE_GP_ID"), gp.getId(), id));
                                gp.setId(id);
                            }
                        }
                        gpr.setGeneProduct(id);
                    });
                    if (gpr.isSetGeneProduct()) {
                        associations.add(gpr);
                    }
                    break;
            }
        }
        return associations;
    }

}
