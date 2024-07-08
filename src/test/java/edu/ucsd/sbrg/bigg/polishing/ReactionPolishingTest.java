package edu.ucsd.sbrg.bigg.polishing;

import edu.ucsd.sbrg.polishing.ReactionPolishing;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.XMLTriple;

import javax.xml.stream.XMLStreamException;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.*;

public class ReactionPolishingTest {

    @Test
    public void noIdReactionIsDeletedFromModel() {
        var model = new Model(3, 2);
        var r = model.createReaction();

        assertEquals(false, r.isSetId());
        assertEquals(false, r.isSetName());
        assertEquals(1, model.getReactionCount());

        var polishing = new ReactionPolishing(r);
        boolean strict = polishing.polish();

        assertEquals(false, strict);
        assertEquals(false, r.isSetId());
        assertEquals(false, r.isSetName());
        assertEquals(0, model.getReactionCount());
    }

    /**
     * Relevant defaults are set.
     * Note that level and version are 3.1 here, so the setting of the fast-attribute
     * can be validated.
     */
    @Test
    public void defaultsAreSet() {
        var model = new Model(3, 1);
        initParameters();
        var r = model.createReaction("some_reaction");

        assertEquals(false, r.isSetName());
        assertEquals(false, r.isSetFast());
        assertEquals(false, r.isSetReversible());
        assertEquals(1, model.getReactionCount());

        var polishing = new ReactionPolishing(r);
        boolean strict = polishing.polish();

        assertEquals(false, strict);
        assertEquals(false, r.isSetName());
        assertEquals(true, r.isSetFast());
        assertEquals(true, r.isSetReversible());
        assertEquals(1, model.getReactionCount());
    }

    /**
     * SBML 3.2 deprecated the 'fast' attribute. The code should be indifferent to that.
     */
    @Test
    public void fastIsOkay() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");

        assertEquals(false, r.isSetFast());

        new ReactionPolishing(r).polish();

        assertEquals(false, r.isSetFast());
    }

    @Test
    public void reactionCompartmentIsSetFromParticipants() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");

        var s1 = model.createSpecies("s1", cytosol);
        r.createReactant(s1);

        assertNull(r.getCompartmentInstance());

        new ReactionPolishing(r).polish();

        assertEquals(cytosol, r.getCompartmentInstance());
    }

    @Test
    public void compartmentIsSetFromPreferentiallyFromProducts() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");
        var extracellular = model.createCompartment("e");

        var s1 = model.createSpecies("s1", cytosol);
        r.createReactant(s1);

        var s2 = model.createSpecies("s2", extracellular);
        r.createProduct(s2);

        assertEquals("", r.getCompartment());

        new ReactionPolishing(r).polish();

        assertEquals(1, r.getReactantCount());
        assertEquals(1, r.getProductCount());
        assertEquals(cytosol, r.getReactant(0).getSpeciesInstance().getCompartmentInstance());
        assertEquals(extracellular, r.getProduct(0).getSpeciesInstance().getCompartmentInstance());
        assertEquals(extracellular, r.getCompartmentInstance());
    }

    @Test
    public void reactionCompartmentIsNotOverriden() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");
        var extracellular = model.createCompartment("e");

        var s1 = model.createSpecies("s1", cytosol);
        r.createReactant(s1);

        r.setCompartment(extracellular);

        new ReactionPolishing(r).polish();

        assertEquals(1, r.getReactantCount());
        assertEquals(extracellular, r.getCompartmentInstance());
    }

    @Test
    public void conflictingProductsLeadToUnset() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");
        var extracellular = model.createCompartment("e");

        var s1 = model.createSpecies("s1", cytosol);
        r.createProduct(s1);

        r.setCompartment(extracellular);

        new ReactionPolishing(r).polish();

        assertEquals(1, r.getProductCount());
        assertEquals(null, r.getCompartmentInstance());
    }

    @Test
    public void inconsistentProductCompartmentsLeadToUnset() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");
        var extracellular = model.createCompartment("e");

        r.setCompartment(cytosol);

        var s2 = model.createSpecies("s2", extracellular);
        r.createProduct(s2);

        var s1 = model.createSpecies("s1", cytosol);
        r.createProduct(s1);

        assertEquals(cytosol, r.getCompartmentInstance());

        new ReactionPolishing(r).polish();

        assertEquals(2, r.getListOfProducts().size());
        assertEquals(Set.of(cytosol, extracellular), r.getListOfProducts().stream()
                .map(SpeciesReference::getSpeciesInstance)
                .map(Species::getCompartmentInstance)
                .collect(Collectors.toSet()));
        assertEquals(null, r.getCompartmentInstance());
    }

    @Test
    public void speciesReferencesWithoutSpeciesAreRetained() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");

        r.setCompartment(cytosol);

        r.createReactant();
        r.createReactant();

        new ReactionPolishing(r).polish();

        assertEquals(2, r.getListOfReactants().size());
        assertEquals(cytosol, r.getCompartmentInstance());
    }

    /**
     * note this documents a bug
     */
    @Test
    public void missingProductSpeciesAreConflictAndLeadToUnset() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");

        r.setCompartment(cytosol);

        r.createProduct();
        r.createProduct();

        new ReactionPolishing(r).polish();

        assertEquals(2, r.getListOfProducts().size());
        assertEquals(null, r.getCompartmentInstance());
    }

    @Test
    public void defaultsAreSetOnSpeciesReferences() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");

        var s1 = model.createSpecies("s1");
        var react1 = r.createReactant(s1);

        var s2 = model.createSpecies("s2");
        var product1 = r.createProduct(s2);

        assertEquals("", react1.getSBOTermID());
        assertEquals("", product1.getSBOTermID());
        assertFalse(react1.isSetConstant());
        assertFalse(product1.isSetConstant());

        new ReactionPolishing(r).polish();

        assertEquals(1, r.getListOfReactants().size());
        assertEquals(1, r.getListOfProducts().size());

        assertEquals("SBO:0000010", react1.getSBOTermID());
        assertEquals("SBO:0000011", product1.getSBOTermID());
        assertTrue(react1.isSetConstant());
        assertTrue(product1.isSetConstant());
    }

    /**
     * Note: this test documents
     * https://github.com/draeger-lab/ModelPolisher/issues/122
     */
    @Test
    public void defaultsAreSetEvenIfCompartmentsAreInconsistent() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");
        var extracellular = model.createCompartment("e");

        var s1 = model.createSpecies("s1", cytosol);
        var product1 = r.createProduct(s1);

        var s2 = model.createSpecies("s2", extracellular);
        var product2 = r.createProduct(s2);

        var s3 = model.createSpecies("s3", cytosol);
        var product3 = r.createProduct(s3);

        assertEquals("", product1.getSBOTermID());
        assertEquals("", product2.getSBOTermID());
        assertEquals("", product3.getSBOTermID());
        assertFalse(product1.isSetConstant());
        assertFalse(product2.isSetConstant());
        assertFalse(product3.isSetConstant());

        new ReactionPolishing(r).polish();

        assertEquals(3, r.getListOfProducts().size());
        assertEquals("SBO:0000011", product1.getSBOTermID());
        assertEquals("SBO:0000011", product2.getSBOTermID());
        assertEquals("SBO:0000011", product3.getSBOTermID());
        assertTrue(product1.isSetConstant());
        assertTrue(product2.isSetConstant());
        assertTrue(product3.isSetConstant());
    }

    /**
     * Note: I consider this behaviour to be a bug basically.
     * <p>
     * I am still testing the behaviour though for validation purposes should
     * this get fixed.
     */
    @Test
    public void objectiveIsCreatedIfNoneIsPresent() {
        var model = new Model(3, 2);
        var fbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        initParameters();

        var r = model.createReaction("some_reaction");
        var kl = r.createKineticLaw();
        var l1 = kl.createLocalParameter("OBJECTIVE_COEFFICIENT");
        l1.setValue(23);

        assertEquals(1, model.getReactionCount());
        assertEquals(0, fbcPlugin.getObjectiveCount());

        new ReactionPolishing(r).polish();

        assertEquals(1, model.getReactionCount());
        assertEquals(1, fbcPlugin.getObjectiveCount());
        assertNotNull(fbcPlugin.getActiveObjectiveInstance());
        var fo = fbcPlugin.getObjective(0)
                .getListOfFluxObjectives()
                .get(0);
        assertEquals("fo_some_reaction", fo.getId());
        assertEquals(r, fo.getReactionInstance());
    }

    @Test
    public void fluxObjectivesAreSetFromKinematicLaws() {
        var model = new Model(3, 2);
        var fbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        initParameters();

        var r = model.createReaction("some_reaction");
        var kl = r.createKineticLaw();
        var l1 = kl.createLocalParameter("OBJECTIVE_COEFFICIENT");
        l1.setValue(23);

        var obj1 = fbcPlugin.createObjective("obj1", Objective.Type.MINIMIZE);
        var obj2 = fbcPlugin.createObjective("obj2", Objective.Type.MAXIMIZE);

        assertEquals(1, model.getReactionCount());
        assertEquals(2, fbcPlugin.getObjectiveCount());
        assertEquals(0, obj1.getFluxObjectiveCount());
        assertEquals(0, obj2.getFluxObjectiveCount());

        new ReactionPolishing(r).polish();

        assertEquals(1, model.getReactionCount());
        assertEquals(2, fbcPlugin.getObjectiveCount());
        assertEquals(1, obj1.getFluxObjectiveCount());
        assertEquals(0, obj2.getFluxObjectiveCount());
        assertNull(fbcPlugin.getActiveObjectiveInstance());
        var fo = obj1
                .getListOfFluxObjectives()
                .get(0);
        assertEquals("fo_some_reaction", fo.getId());
        assertEquals(r, fo.getReactionInstance());
        assertEquals(23, fo.getCoefficient());
    }

    @Test
    public void existingObjectivesAreRespected() {
        var model = new Model(3, 2);
        var fbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        initParameters();

        var r = model.createReaction("some_reaction");
        var kl = r.createKineticLaw();
        var l1 = kl.createLocalParameter("OBJECTIVE_COEFFICIENT");
        l1.setValue(23);

        var obj1 = fbcPlugin.createObjective("obj1", Objective.Type.MINIMIZE);
        var existing_fo = obj1.createFluxObjective("existing_fo");
        existing_fo.setReaction(r);
        var obj2 = fbcPlugin.createObjective("obj2", Objective.Type.MAXIMIZE);

        assertEquals(1, model.getReactionCount());
        assertEquals(2, fbcPlugin.getObjectiveCount());
        assertEquals(1, obj1.getFluxObjectiveCount());
        assertEquals(0, obj2.getFluxObjectiveCount());

        new ReactionPolishing(r).polish();

        assertEquals(1, model.getReactionCount());
        assertEquals(2, fbcPlugin.getObjectiveCount());
        assertEquals(1, obj1.getFluxObjectiveCount());
        assertEquals(0, obj2.getFluxObjectiveCount());
        assertNull(fbcPlugin.getActiveObjectiveInstance());
        var fo = obj1
                .getListOfFluxObjectives()
                .get(0);
        assertEquals("existing_fo", fo.getId());
        assertEquals(r, fo.getReactionInstance());
        assertFalse(fo.isSetCoefficient());
    }

    @Test
    public void boundsAreCreatedFromLocalParameters() {
        var model = new Model(3, 2);
        initParameters();

        var r = model.createReaction("some_reaction");
        var rFbcPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

        var kl = r.createKineticLaw();
        kl.createLocalParameter("LOWER_BOUND");
        kl.createLocalParameter("UPPER_BOUND");

        assertNull(rFbcPlugin.getLowerFluxBoundInstance());
        assertNull(rFbcPlugin.getUpperFluxBoundInstance());

        new ReactionPolishing(r).polish();

        var lb = rFbcPlugin.getLowerFluxBoundInstance();
        assertEquals("some_reaction_LOWER_BOUND", lb.getId());
        assertTrue(lb.isSetConstant());
        assertTrue(lb.isConstant());
        assertEquals("SBO:0000625", lb.getSBOTermID());
        var ub = rFbcPlugin.getUpperFluxBoundInstance();
        assertEquals("some_reaction_UPPER_BOUND", ub.getId());
        assertTrue(ub.isSetConstant());
        assertTrue(ub.isConstant());
        assertEquals("SBO:0000625", lb.getSBOTermID());
    }

    @Test
    public void existingBoundsGetDefaults() {
        var model = new Model(3, 2);
        initParameters();

        var r = model.createReaction("some_reaction");
        var rFbcPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

        var kl = r.createKineticLaw();
        kl.createLocalParameter("LOWER_BOUND");
        kl.createLocalParameter("UPPER_BOUND");

        var p1 = model.createParameter("default_blubb");
        rFbcPlugin.setLowerFluxBound(p1);
        var p2 = model.createParameter("other_blubb");
        rFbcPlugin.setUpperFluxBound(p2);

        new ReactionPolishing(r).polish();

        var lb = rFbcPlugin.getLowerFluxBoundInstance();
        assertEquals("default_blubb", lb.getId());
        assertFalse(lb.isSetConstant());
        assertEquals("SBO:0000626", lb.getSBOTermID());
        var ub = rFbcPlugin.getUpperFluxBoundInstance();
        assertEquals("other_blubb", ub.getId());
        assertFalse(ub.isSetConstant());
        assertEquals("SBO:0000625", ub.getSBOTermID());
    }

    @Test
    public void newBoundsSettingsByValueAndExistingParameters() {
        var model = new Model(3, 2);
        initParameters();

        var r = model.createReaction("some_reaction");
        var rFbcPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

        var kl = r.createKineticLaw();
        var p1 = kl.createLocalParameter("LOWER_BOUND");
        p1.setValue(1000);
        kl.createLocalParameter("UPPER_BOUND");

        var pModel = model.createParameter("some_reaction_UPPER_BOUND");

        assertNull(rFbcPlugin.getLowerFluxBoundInstance());
        assertNull(rFbcPlugin.getUpperFluxBoundInstance());

        new ReactionPolishing(r).polish();

        var lb = rFbcPlugin.getLowerFluxBoundInstance();
        assertEquals("DEFAULT_UPPER_BOUND", lb.getId());
        var ub = rFbcPlugin.getUpperFluxBoundInstance();
        assertEquals("some_reaction_UPPER_BOUND", ub.getId());
        assertEquals(pModel, ub);
    }

    /**
     * Note: this apparently shows a bug in the GPRParser::areEqual method.
     * That is, hopefully, once that bug is fixed, this should fail.
     *
     * @throws XMLStreamException
     */
    @Test
    public void notesToGPRs() throws XMLStreamException {
        var model = new Model(3, 2);
        initParameters();

        var r = model.createReaction("some_reaction");
        var rFbcPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
        var body = new XMLNode(new XMLTriple("body", "", ""));
        var p1 = new XMLNode(new XMLTriple("p", "", ""));
        p1.addChild(new XMLNode("GENE_ASSOCIATION: some_assoc"));
        body.addChild(p1);
        var p2 = new XMLNode(new XMLTriple("p", "", ""));
        p2.addChild(new XMLNode("GENE_ASSOCIATION: some_other_assoc"));
        body.addChild(p2);
        r.setNotes(body);

        new ReactionPolishing(r).polish();

        assertTrue(rFbcPlugin.getGeneProductAssociation()
                .getAssociation() instanceof GeneProductRef,
                "The result is expected to be " +
                        "(wrongly, happy fixing should this fail for you!) " +
                        "a single gene association, not an AND or OR.");
        assertEquals("G_some_assoc",
                ((GeneProductRef) rFbcPlugin.getGeneProductAssociation()
                        .getAssociation()).getGeneProduct());
    }

}
