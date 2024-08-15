package edu.ucsd.sbrg.polishing.ext.fbc;

import de.zbit.util.ResourceManager;
import edu.ucsd.sbrg.db.bigg.BiGGId;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.xml.XMLNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.text.MessageFormat.format;

public class GeneProductAssociationsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GeneProductAssociationsProcessor.class);
    private static final ResourceBundle MESSAGES = ResourceManager.getBundle("edu.ucsd.sbrg.polisher.Messages");

    private final Map<String, XMLNode> oldGeneAssociations = new HashMap<>();


    public void convertAssociationsToFBCV2(Reaction reaction, boolean addGenericTerms) {
        var model = reaction.getModel();
        String id = reaction.getId();
        XMLNode ga = oldGeneAssociations.getOrDefault(id, null);
        if (ga != null) {
            var reactionPlugin = (FBCReactionPlugin) reaction.getPlugin(FBCConstants.shortLabel);
            var gpa = new GeneProductAssociation(reaction.getLevel(), reaction.getVersion());
            List<Association> associations = processAssociation(ga, model, addGenericTerms);
            if (associations.size() == 1) {
                gpa.setAssociation(associations.get(0));
                reactionPlugin.setGeneProductAssociation(gpa);
            }
        }
    }

    // TODO: das war extrahiert aus convertAssociationsToFBCV2 und muss aufgerufen werden noch
    private boolean processNonRDFGeneAssociationAnnotations(Model model) {
        var annotation = model.getAnnotation();
        XMLNode node = annotation.getNonRDFannotation();
        if (node == null) {
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            XMLNode current = node.getChild(i);
            if (current.getName().equals("geneAssociation")) {
                String reactionId = current.getAttributes().getValue("reaction");
                oldGeneAssociations.put(reactionId, node.getChild(i));
                node.removeChild(i);
            }
        }
        return false;
    }

    private List<Association> processAssociation(XMLNode association, Model model, boolean addGenericTerms) {
        int level = model.getLevel(), version = model.getVersion();
        List<Association> associations = new ArrayList<>();
        for (int i = 0; i < association.getChildCount(); i++) {
            XMLNode current = association.getChild(i);
            switch (current.getName()) {
                case "and":
                    And and = new And(level, version);
                    if (addGenericTerms) {
                        and.setSBOTerm(173); // AND
                    }
                    and.addAllAssociations(processAssociation(current, model, addGenericTerms));
                    if (and.isSetListOfAssociations()) {
                        associations.add(and);
                    }
                    break;
                case "or":
                    Or or = new Or(level, version);
                    if (!addGenericTerms) {
                        or.setSBOTerm(174); // OR
                    }
                    or.addAllAssociations(processAssociation(current, model, addGenericTerms));
                    if (or.isSetListOfAssociations()) {
                        associations.add(or);
                    }
                    break;
                case "gene":
                    String geneReference = current.getAttributes().getValue("reference");
                    GeneProductRef gpr = new GeneProductRef(level, version);
                    var id = BiGGId.createGeneId(geneReference).toBiGGId();
                    if (!model.containsUniqueNamedSBase(id)) {
                        GeneProduct gp = (GeneProduct) model.findUniqueNamedSBase(id);
                        if (gp == null) {
                            logger.debug(format("Creating missing gene product {0}", id));
                            FBCModelPlugin fbcPlug = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
                            gp = fbcPlug.createGeneProduct(id);
                            gp.setLabel(id);
                        } else {
                            logger.info(format(MESSAGES.getString("UPDATE_GP_ID"), gp.getId(), id));
                            gp.setId(id);
                        }
                    }
                    gpr.setGeneProduct(id);
                    if (gpr.isSetGeneProduct()) {
                        associations.add(gpr);
                    }
                    break;
            }
        }
        return associations;
    }

}
