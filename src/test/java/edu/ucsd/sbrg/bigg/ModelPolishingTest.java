package edu.ucsd.sbrg.bigg;

import de.zbit.util.prefs.SBProperties;
import de.zbit.util.progressbar.ProgressBar;
import edu.ucsd.sbrg.bigg.polishing.ModelPolishing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxObjective;
import org.sbml.jsbml.ext.fbc.ListOfObjectives;
import org.sbml.jsbml.ext.fbc.Objective;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class ModelPolishingTest {

    private void initParameters(String fluxObjectives) {
        SBProperties props = new SBProperties();
        props.setProperty("INPUT", "bla");
        props.setProperty("OUTPUT", "bla");
        if( null != fluxObjectives ) {
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
        Model m = new Model();
        FBCModelPlugin fbcPlugin = new FBCModelPlugin(m);

        m.addPlugin("fbc", fbcPlugin);
        ListOfObjectives loo = new ListOfObjectives();
        Objective o1 = new Objective("obj1");
        Objective o2 = new Objective("obj2");
        fbcPlugin.setActiveObjective(o2);
        FluxObjective o2FlOb = new FluxObjective();
        o2.addFluxObjective(o2FlOb);
        Objective o3 = new Objective("obj3");
        loo.add(o1);
        loo.add(o2);
        loo.add(o3);
        loo.setActiveObjective(o2);
        fbcPlugin.setListOfObjectives(loo);
        fbcPlugin.setActiveObjective(o2);

        m.getListOfReactions().add(new Reaction("objective_reaction1"));
        m.getListOfReactions().add(new Reaction("yadda_Biomass_yadda"));

        initParameters(" objective_reaction1 ");

        ModelPolishing polishing = new ModelPolishing(m, false, new ProgressBar(0));
        polishing.polish();

        assertEquals("obj2", fbcPlugin.getActiveObjective());
    }

    /**
     * Test that if objectives don't have flux objectives set and viable arguments are supplied,
     * those are used to add flux objectives to the active objective.
     *
     * Note that this is the behaviour as I found it, the test only serves to ensure I don't inadvertently
     * change behaviour.
     */
    @Test
    public void argumentsAreUsedToInferFluxObjectives() {
        Model m = new Model();
        m.setLevel(3);
        m.setVersion(2);
        FBCModelPlugin fbcPlugin = new FBCModelPlugin(m);

        m.addPlugin("fbc", fbcPlugin);
        ListOfObjectives loo = new ListOfObjectives();
        // yes, this is actually required downstream
        loo.setLevel(3);
        loo.setVersion(2);
        Objective o1 = new Objective("obj1");
        Objective o2 = new Objective("obj2");
        Objective o3 = new Objective("obj3");
        o3.setLevel(3);
        o3.setVersion(2);
        loo.add(o1);
        loo.add(o2);
        loo.add(o3);
        loo.setActiveObjective(o3);
        fbcPlugin.setListOfObjectives(loo);
        fbcPlugin.setActiveObjective(o3);

        m.getListOfReactions().add(new Reaction("objective_reaction1"));
        m.getListOfReactions().add(new Reaction("objective_reaction2"));
        m.getListOfReactions().add(new Reaction("yadda_Biomass_yadda"));

        initParameters(" objective_reaction1:objective_reaction2 ");

        ModelPolishing polishing = new ModelPolishing(m, false, new ProgressBar(0));
        polishing.polish();

        assertEquals("obj3", fbcPlugin.getActiveObjective());
        assertEquals(2, fbcPlugin.getListOfObjectives()
                .getActiveObjectiveInstance()
                .getFluxObjectiveCount());
        List<FluxObjective> fos = fbcPlugin.getListOfObjectives()
                .getActiveObjectiveInstance()
                .getListOfFluxObjectives();
        assertEquals("objective_reaction1", fos
                .get(0)
                .getReaction());
        assertEquals("objective_reaction2", fos
                .get(1)
                .getReaction());
    }

    /**
     * Test that if objectives don't have flux objectives set and no (viable) arguments are supplied,
     * the biomass function is used to add flux objectives to the active objective.
     *
     * Note that this is the behaviour as I found it, the test only serves to ensure I don't inadvertently
     * change behaviour.
     */
    @Test
    public void biomassIsUsedToInferFluxObjectives() {
        Model m = new Model();
        m.setLevel(3);
        m.setVersion(2);
        FBCModelPlugin fbcPlugin = new FBCModelPlugin(m);

        m.addPlugin("fbc", fbcPlugin);
        ListOfObjectives loo = new ListOfObjectives();
        // yes, this is actually required downstream
        loo.setLevel(3);
        loo.setVersion(2);
        Objective o1 = new Objective("obj1");
        Objective o2 = new Objective("obj2");
        Objective o3 = new Objective("obj3");
        o2.setLevel(3);
        o2.setVersion(2);
        loo.add(o1);
        loo.add(o2);
        loo.add(o3);
        loo.setActiveObjective(o2);
        fbcPlugin.setListOfObjectives(loo);
        fbcPlugin.setActiveObjective(o2);

        m.getListOfReactions().add(new Reaction("yadda_Biomass_yadda"));

        initParameters(null);

        ModelPolishing polishing = new ModelPolishing(m, false, new ProgressBar(0));
        polishing.polish();

        assertEquals("obj2", fbcPlugin.getActiveObjective());
        assertEquals(1, fbcPlugin.getListOfObjectives()
                .getActiveObjectiveInstance()
                .getFluxObjectiveCount());
        assertEquals("yadda_Biomass_yadda", fbcPlugin.getListOfObjectives()
                .getActiveObjectiveInstance()
                .getListOfFluxObjectives()
                .get(0)
                .getReaction());
    }
}