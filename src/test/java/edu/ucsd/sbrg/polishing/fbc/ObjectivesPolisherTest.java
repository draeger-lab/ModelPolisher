package edu.ucsd.sbrg.polishing.fbc;

import edu.ucsd.sbrg.parameters.FluxObjectivesPolishingParameters;
import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.polishing.ext.fbc.ObjectivesPolisher;
import edu.ucsd.sbrg.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class ObjectivesPolisherTest {

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

        var parameters = new PolishingParameters(
                null,
                new FluxObjectivesPolishingParameters(
                        null,
                        List.of(" objective_reaction1 ")),
                false);

        m.createReaction("objective_reaction1");
        m.createReaction("yadda_Biomass_yadda");

        var mPlug = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
        new ObjectivesPolisher(fbcPlugin, parameters, new IdentifiersOrg()).polish(mPlug.getListOfObjectives());

        assertEquals("obj2", fbcPlugin.getActiveObjective());
    }




}