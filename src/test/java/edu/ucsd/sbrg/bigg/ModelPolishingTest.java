package edu.ucsd.sbrg.bigg;

import de.zbit.util.prefs.SBProperties;
import de.zbit.util.progressbar.ProgressBar;
import edu.ucsd.sbrg.bigg.polishing.ModelPolishing;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class ModelPolishingTest {

    private void initParameters(String fluxObjectives) {
        var props = new SBProperties();
        props.setProperty("INPUT", "bla");
        props.setProperty("OUTPUT", "bla");
        if (null != fluxObjectives) {
            props.setProperty("FLUX_OBJECTIVES", fluxObjectives);
        }
        props.setProperty("COMPRESSION_TYPE", ModelPolisherOptions.Compression.NONE.name());
        Parameters.parameters = null;
        Parameters.init(props);
    }

    /**
     * Test that if an objective is set and it has flux objectives too, it won't be overwritten.
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

        m.createReaction("objective_reaction1");
        m.createReaction("yadda_Biomass_yadda");

        initParameters(" objective_reaction1 ");

        var polishing = new ModelPolishing(m, false, new ProgressBar(0));
        polishing.polish();

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

        m.createReaction("objective_reaction1");
        m.createReaction("objective_reaction2");
        m.createReaction("yadda_Biomass_yadda");

        initParameters(" objective_reaction1:objective_reaction2 ");

        var polishing = new ModelPolishing(m, false, new ProgressBar(0));
        polishing.polish();

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

        initParameters(null);

        var polishing = new ModelPolishing(m, false, new ProgressBar(0));
        polishing.polish();

        assertEquals("obj2", fbcPlugin.getActiveObjective());
        assertEquals(Set.of("yadda_Biomass_yadda"),
                fbcPlugin.getListOfObjectives()
                        .getActiveObjectiveInstance()
                        .getListOfFluxObjectives()
                        .stream()
                        .map(FluxObjective::getReaction)
                        .collect(Collectors.toSet()));
    }
}