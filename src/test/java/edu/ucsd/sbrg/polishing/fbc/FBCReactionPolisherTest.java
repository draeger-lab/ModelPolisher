package edu.ucsd.sbrg.polishing.fbc;

import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.parameters.SBOParameters;
import edu.ucsd.sbrg.polishing.ext.fbc.FBCReactionPolisher;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.*;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.XMLTriple;

import static org.junit.jupiter.api.Assertions.*;

class FBCReactionPolisherTest {

    public final SBOParameters sboParameters = new SBOParameters();
    private final PolishingParameters polishingParameters = new PolishingParameters();

    @Test
    public void existingBoundsGetDefaults() {
        var model = new Model(3, 2);
        var modelFbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
        var r = model.createReaction("some_reaction");
        var rFbcPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

        var kl = r.createKineticLaw();
        kl.createLocalParameter("LOWER_BOUND");
        kl.createLocalParameter("UPPER_BOUND");

        var p1 = model.createParameter("default_blubb");
        rFbcPlugin.setLowerFluxBound(p1);
        var p2 = model.createParameter("other_blubb");
        rFbcPlugin.setUpperFluxBound(p2);

        new FBCReactionPolisher(modelFbcPlugin, polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

        var lb = rFbcPlugin.getLowerFluxBoundInstance();
        assertEquals("default_blubb", lb.getId());
        assertFalse(lb.isSetConstant());
        assertEquals("SBO:0000626", lb.getSBOTermID());
        var ub = rFbcPlugin.getUpperFluxBoundInstance();
        assertEquals("other_blubb", ub.getId());
        assertFalse(ub.isSetConstant());
        assertEquals("SBO:0000625", ub.getSBOTermID());
    }


//    /**
//     * Note: I consider this behaviour to be a bug basically.
//     * <p>
//     * I am still testing the behaviour though for validation purposes should
//     * this get fixed.
//     */
//    @Test
//    public void objectiveIsCreatedIfNoneIsPresent() {
//        var model = new Model(3, 2);
//        var fbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);
//
//        var r = model.createReaction("some_reaction");
//        var kl = r.createKineticLaw();
//        var l1 = kl.createLocalParameter("OBJECTIVE_COEFFICIENT");
//        l1.setValue(23);
//
//        assertEquals(1, model.getReactionCount());
//        assertEquals(0, fbcPlugin.getObjectiveCount());
//
//        new FBCReactionPolisher(fbcPlugin, polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);
//
//        assertEquals(1, model.getReactionCount());
//        assertEquals(1, fbcPlugin.getObjectiveCount());
//        assertNotNull(fbcPlugin.getActiveObjectiveInstance());
//        var fo = fbcPlugin.getObjective(0)
//                .getListOfFluxObjectives()
//                .get(0);
//        assertEquals("fo_some_reaction", fo.getId());
//        assertEquals(r, fo.getReactionInstance());
//    }


    @Test
    public void fluxObjectivesAreSetFromKinematicLaws() {
        var model = new Model(3, 2);
        var fbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);

        var r = model.createReaction("some_reaction");
        var kl = r.createKineticLaw();
        var l1 = kl.createLocalParameter("OBJECTIVE_COEFFICIENT");
        l1.setValue(23);

        var obj1 = fbcPlugin.createObjective("obj1", Objective.Type.MINIMIZE);
        var obj2 = fbcPlugin.createObjective("obj2", Objective.Type.MAXIMIZE);
        fbcPlugin.setActiveObjective(obj1);

        assertEquals(1, model.getReactionCount());
        assertEquals(2, fbcPlugin.getObjectiveCount());
        assertEquals(0, obj1.getFluxObjectiveCount());
        assertEquals(0, obj2.getFluxObjectiveCount());

        new FBCReactionPolisher(fbcPlugin, polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

        assertEquals(1, model.getReactionCount());
        assertEquals(2, fbcPlugin.getObjectiveCount());
        assertEquals(1, obj1.getFluxObjectiveCount());
        assertEquals(0, obj2.getFluxObjectiveCount());
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

        var r = model.createReaction("some_reaction");
        var kl = r.createKineticLaw();
        var l1 = kl.createLocalParameter("OBJECTIVE_COEFFICIENT");
        l1.setValue(23);

        var obj1 = fbcPlugin.createObjective("obj1", Objective.Type.MINIMIZE);
        var existing_fo = obj1.createFluxObjective("existing_fo");
        existing_fo.setReaction(r);
        var obj2 = fbcPlugin.createObjective("obj2", Objective.Type.MAXIMIZE);
        fbcPlugin.setActiveObjective(obj1);

        assertEquals(1, model.getReactionCount());
        assertEquals(2, fbcPlugin.getObjectiveCount());
        assertEquals(1, obj1.getFluxObjectiveCount());
        assertEquals(0, obj2.getFluxObjectiveCount());

        new FBCReactionPolisher(fbcPlugin, polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

        assertEquals(1, model.getReactionCount());
        assertEquals(2, fbcPlugin.getObjectiveCount());
        assertEquals(1, obj1.getFluxObjectiveCount());
        assertEquals(0, obj2.getFluxObjectiveCount());
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
        var fbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);

        var r = model.createReaction("some_reaction");
        var rFbcPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

        var kl = r.createKineticLaw();
        kl.createLocalParameter("LOWER_BOUND");
        kl.createLocalParameter("UPPER_BOUND");

        assertNull(rFbcPlugin.getLowerFluxBoundInstance());
        assertNull(rFbcPlugin.getUpperFluxBoundInstance());

        new FBCReactionPolisher(fbcPlugin, polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

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
    public void newBoundsSettingsByValueAndExistingParameters() {
        var model = new Model(3, 2);
        var fbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);

        var r = model.createReaction("some_reaction");
        var rFbcPlugin = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);

        var kl = r.createKineticLaw();
        var p1 = kl.createLocalParameter("LOWER_BOUND");
        p1.setValue(1000);
        kl.createLocalParameter("UPPER_BOUND");

        var pModel = model.createParameter("some_reaction_UPPER_BOUND");

        assertNull(rFbcPlugin.getLowerFluxBoundInstance());
        assertNull(rFbcPlugin.getUpperFluxBoundInstance());

        new FBCReactionPolisher(fbcPlugin, polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

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
        var fbcPlugin = (FBCModelPlugin) model.getPlugin(FBCConstants.shortLabel);

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

        new FBCReactionPolisher(fbcPlugin, polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

        assertInstanceOf(GeneProductRef.class, rFbcPlugin.getGeneProductAssociation()
                .getAssociation(), "The result is expected to be " +
                "(wrongly, happy fixing should this fail for you!) " +
                "a single gene association, not an AND or OR.");
        assertEquals("G_some_assoc",
                ((GeneProductRef) rFbcPlugin.getGeneProductAssociation()
                        .getAssociation()).getGeneProduct());
    }

}