package edu.ucsd.sbrg.polishing.fbc;

import edu.ucsd.sbrg.Parameters;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.ucsd.sbrg.TestUtils.initParameters;
import static org.junit.jupiter.api.Assertions.*;


public class FluxObjectivesPolisherTest {

    private final Parameters parameters = initParameters();

    /**
     * Test that if an objective is set, and it has flux objectives too, it won't be overwritten.
     */
    @Test
    public void activeObjectiveRemainsUnchangedIfItHasFluxObjective() {
        var m = new Model(3,2);
        var fbcPlugin = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);

        fbcPlugin.createObjective("obj1");
        var o2 = fbcPlugin.createObjective("obj2");
        o2.createFluxObjective();
        fbcPlugin.setActiveObjective(o2);
        fbcPlugin.createObjective("obj3");

        var parameters = initParameters(Map.of("FLUX_OBJECTIVES", " objective_reaction1 "));

        m.createReaction("objective_reaction1");
        m.createReaction("yadda_Biomass_yadda");

        var mPlug = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
        new FluxObjectivesPolisher(mPlug, parameters).polish(mPlug.getListOfObjectives());

        assertEquals("obj2", fbcPlugin.getActiveObjective());
    }

    /**
     * Test that if objectives don't have flux objectives set and viable arguments are supplied,
     * those are used to add flux objectives to the active objective.
     * <p>
     * Note that this is the behaviour as I found it, the test only serves to ensure I don't inadvertently
     * change behaviour.
     */
    @Test
    public void argumentsAreUsedToInferFluxObjectives() {
        var m = new Model(3,2);
        var fbcPlugin = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);

        fbcPlugin.createObjective("obj1");
        fbcPlugin.createObjective("obj2");
        var o3 = fbcPlugin.createObjective("obj3");
        fbcPlugin.setActiveObjective(o3);
        assertNotNull(fbcPlugin.getActiveObjectiveInstance());

        m.createReaction("objective_reaction1");
        m.createReaction("objective_reaction2");
        m.createReaction("yadda_Biomass_yadda");

        var parameters = initParameters(Map.of("fluxObjectives",
                List.of("objective_reaction1", "objective_reaction2")));

        new FluxObjectivesPolisher(fbcPlugin, parameters).polish(fbcPlugin.getListOfObjectives());

        assertEquals("obj3", fbcPlugin.getActiveObjective());
        assertEquals(2, fbcPlugin.getListOfObjectives()
                .getActiveObjectiveInstance()
                .getFluxObjectiveCount());
        var fos = fbcPlugin.getListOfObjectives()
                .getActiveObjectiveInstance()
                .getListOfFluxObjectives()
                .stream()
                .map(FluxObjective::getReaction)
                .collect(Collectors.toList());
        assertEquals(List.of("objective_reaction1", "objective_reaction2"), fos);
    }

    /**
     * Test that if objectives don't have flux objectives set and no (viable) arguments are supplied,
     * the biomass function is used to add flux objectives to the active objective.
     * <p>
     * Note that this is the behaviour as I found it, the test only serves to ensure I don't inadvertently
     * change behaviour.
     */
    @Test
    public void biomassIsUsedToInferFluxObjectives() {
        var m = new Model(3,2);
        var fbcPlugin = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);

        m.addPlugin("fbc", fbcPlugin);

        fbcPlugin.createObjective("obj1");
        var o2 = fbcPlugin.createObjective("obj2");
        fbcPlugin.setActiveObjective(o2);
        fbcPlugin.createObjective("obj3");

        m.createReaction("yadda_Biomass_yadda");

        var mPlug = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
        new FluxObjectivesPolisher(mPlug, initParameters()).polish(mPlug.getListOfObjectives());

        assertEquals("obj2", fbcPlugin.getActiveObjective());
        assertEquals(Set.of("yadda_Biomass_yadda"),
                fbcPlugin.getListOfObjectives()
                        .getActiveObjectiveInstance()
                        .getListOfFluxObjectives()
                        .stream()
                        .map(FluxObjective::getReaction)
                        .collect(Collectors.toSet()));
    }

    /**
     * Test to ensure that objectives without any flux objectives are removed from the model.
     * This test initializes a model with a single objective that has no flux objectives set,
     * runs the polishing process, and then checks that the objective count is zero.
     */
    @Test
    public void emptyObjectivesAreRemoved() {
        var m = new Model(3,2);
        var fbcPlugin = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);

        var o1 = fbcPlugin.createObjective("obj1");
        fbcPlugin.setActiveObjective(o1);
        // o1.setListOfFluxObjectives(new ListOf<>());

        var mPlug = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
        new FluxObjectivesPolisher(mPlug, initParameters()).polish(mPlug.getListOfObjectives());

        assertEquals(0, fbcPlugin.getObjectiveCount());
    }
}