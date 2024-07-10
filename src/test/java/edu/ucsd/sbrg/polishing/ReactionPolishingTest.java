package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.util.GeneProductAssociationsPolisher;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.XMLTriple;

import java.util.Set;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.*;

public class ReactionPolishingTest {

    private final Parameters parameters = initParameters();

    @Test
    public void noIdReactionIsDeletedFromModel() {
        var model = new Model(3, 2);
        var r = model.createReaction();

        assertFalse(r.isSetId());
        assertFalse(r.isSetName());
        assertEquals(1, model.getReactionCount());

        var polisher = new ModelPolisher(parameters);
        polisher.polishListOfReactions(model);

        var gpaPolisher = new GeneProductAssociationsPolisher();
        boolean strict = model.getListOfReactions()
                .stream()
                .allMatch(reaction ->
                        new ReactionPolishing(reaction, gpaPolisher, parameters).checkReactionStrictness());

        assertTrue(strict);
        assertFalse(r.isSetId());
        assertFalse(r.isSetName());
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

        assertFalse(r.isSetName());
        assertFalse(r.isSetFast());
        assertFalse(r.isSetReversible());
        assertEquals(1, model.getReactionCount());

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();
        boolean strict = polishing.checkReactionStrictness();

        assertFalse(strict);
        assertFalse(r.isSetName());
        assertTrue(r.isSetFast());
        assertTrue(r.isSetReversible());
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

        assertFalse(r.isSetFast());

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

        assertFalse(r.isSetFast());
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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

        assertEquals(1, r.getReactantCount());
        assertEquals(1, r.getProductCount());
        assertEquals(cytosol, r.getReactant(0).getSpeciesInstance().getCompartmentInstance());
        assertEquals(extracellular, r.getProduct(0).getSpeciesInstance().getCompartmentInstance());
        assertEquals(extracellular, r.getCompartmentInstance());
    }

    @Test
    public void reactionCompartmentIsNotOverridden() {
        var model = new Model(3, 2);
        initParameters();
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");
        var extracellular = model.createCompartment("e");

        var s1 = model.createSpecies("s1", cytosol);
        r.createReactant(s1);

        r.setCompartment(extracellular);

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

        assertEquals(1, r.getProductCount());
        assertNull(r.getCompartmentInstance());
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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

        assertEquals(2, r.getListOfProducts().size());
        assertEquals(Set.of(cytosol, extracellular), r.getListOfProducts().stream()
                .map(SpeciesReference::getSpeciesInstance)
                .map(Species::getCompartmentInstance)
                .collect(Collectors.toSet()));
        assertNull(r.getCompartmentInstance());
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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

        assertEquals(2, r.getListOfProducts().size());
        assertNull(r.getCompartmentInstance());
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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

        assertEquals(1, r.getListOfReactants().size());
        assertEquals(1, r.getListOfProducts().size());

        assertEquals("SBO:0000010", react1.getSBOTermID());
        assertEquals("SBO:0000011", product1.getSBOTermID());
        assertTrue(react1.isSetConstant());
        assertTrue(product1.isSetConstant());
    }

    /**
     * Note: this test documents
     * <a href="https://github.com/draeger-lab/ModelPolisher/issues/122">...</a>
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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

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
     */
    @Test
    public void notesToGPRs() {
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

        var polishing = new ReactionPolishing(r, new GeneProductAssociationsPolisher(), parameters);
        polishing.polish();

        assertInstanceOf(GeneProductRef.class, rFbcPlugin.getGeneProductAssociation()
                .getAssociation(), "The result is expected to be " +
                "(wrongly, happy fixing should this fail for you!) " +
                "a single gene association, not an AND or OR.");
        assertEquals("G_some_assoc",
                ((GeneProductRef) rFbcPlugin.getGeneProductAssociation()
                        .getAssociation()).getGeneProduct());
    }

}
