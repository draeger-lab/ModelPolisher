package de.uni_halle.informatik.biodata.mp.polishing;

import de.uni_halle.informatik.biodata.mp.io.ModelReader;
import de.uni_halle.informatik.biodata.mp.io.ModelReaderException;
import de.uni_halle.informatik.biodata.mp.parameters.PolishingParameters;
import de.uni_halle.informatik.biodata.mp.parameters.SBOParameters;
import de.uni_halle.informatik.biodata.mp.polishing.ext.fbc.StrictnessPredicate;
import de.uni_halle.informatik.biodata.mp.resolver.identifiersorg.IdentifiersOrg;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class ReactionsPolisherTest {

    private final SBOParameters sboParameters = new SBOParameters();
    private final PolishingParameters polishingParameters = new PolishingParameters();

    private SBMLDocument model1507180049 = new ModelReader(sboParameters, new IdentifiersOrg()).read(
            new File(ReactionsPolisherTest.class.getClassLoader().getResource("de/uni_halle/informatik/biodata/mp/models/MODEL1507180049.xml").getFile()));

    public ReactionsPolisherTest() throws ModelReaderException {
    }

    /**
     * Relevant defaults are set.
     * Note that level and version are 3.1 here, so the setting of the fast-attribute
     * can be validated.
     */
    @Test
    public void defaultsAreSet() {
        var model = new Model(3, 1);
        var r = model.createReaction("some_reaction");

        assertFalse(r.isSetName());
        assertEquals(1, model.getReactionCount());

        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);
        boolean strict = new StrictnessPredicate().test(model);

        assertFalse(strict);
        assertFalse(r.isSetName());
        assertEquals(1, model.getReactionCount());
    }

    /**
     * SBML 3.2 deprecated the 'fast' attribute. The code should be indifferent to that.
     */
    @Test
    public void fastIsOkay() {
        var model = new Model(3, 2);
        var r = model.createReaction("some_reaction");

        assertFalse(r.isSetFast());

        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

        assertFalse(r.isSetFast());
    }

    @Test
    public void reactionCompartmentIsSetFromParticipants() {
        var model = new Model(3, 2);
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");

        var s1 = model.createSpecies("s1", cytosol);
        r.createReactant(s1);

        assertNull(r.getCompartmentInstance());

        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

        assertEquals(cytosol, r.getCompartmentInstance());
    }

//    @Test
//    public void compartmentIsSetFromPreferentiallyFromProducts() {
//        var model = new Model(3, 2);
//        var r = model.createReaction("some_reaction");
//        var cytosol = model.createCompartment("c");
//        var extracellular = model.createCompartment("e");
//
//        var s1 = model.createSpecies("s1", cytosol);
//        r.createReactant(s1);
//
//        var s2 = model.createSpecies("s2", extracellular);
//        r.createProduct(s2);
//
//        assertEquals("", r.getCompartment());
//
//        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);
//
//        assertEquals(1, r.getReactantCount());
//        assertEquals(1, r.getProductCount());
//        assertEquals(cytosol, r.getReactant(0).getSpeciesInstance().getCompartmentInstance());
//        assertEquals(extracellular, r.getProduct(0).getSpeciesInstance().getCompartmentInstance());
//        assertEquals(extracellular, r.getCompartmentInstance());
//    }

    @Test
    public void reactionCompartmentIsNotOverridden() {
        var model = new Model(3, 2);
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");
        var extracellular = model.createCompartment("e");

        var s1 = model.createSpecies("s1", cytosol);
        r.createReactant(s1);

        r.setCompartment(extracellular);

        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

        assertEquals(1, r.getReactantCount());
        assertEquals(extracellular, r.getCompartmentInstance());
    }

//    @Test
//    public void conflictingProductsLeadToUnset() {
//        var model = new Model(3, 2);
//        var r = model.createReaction("some_reaction");
//        var cytosol = model.createCompartment("c");
//        var extracellular = model.createCompartment("e");
//
//        var s1 = model.createSpecies("s1", cytosol);
//        r.createProduct(s1);
//
//        r.setCompartment(extracellular);
//
//        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);
//
//        assertEquals(1, r.getProductCount());
//        assertNull(r.getCompartmentInstance());
//    }

//    @Test
//    public void inconsistentProductCompartmentsLeadToUnset() {
//        var model = new Model(3, 2);
//        var r = model.createReaction("some_reaction");
//        var cytosol = model.createCompartment("c");
//        var extracellular = model.createCompartment("e");
//
//        r.setCompartment(cytosol);
//
//        var s2 = model.createSpecies("s2", extracellular);
//        r.createProduct(s2);
//
//        var s1 = model.createSpecies("s1", cytosol);
//        r.createProduct(s1);
//
//        assertEquals(cytosol, r.getCompartmentInstance());
//
//        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);
//
//        assertEquals(2, r.getListOfProducts().size());
//        assertEquals(cytosol, r.getProduct("s1").getSpeciesInstance().getCompartmentInstance());
//        assertEquals(extracellular, r.getProduct("s2").getSpeciesInstance().getCompartmentInstance());
//        assertEquals(cytosol, r.getCompartmentInstance());
//    }

    @Test
    public void speciesReferencesWithoutSpeciesAreRetained() {
        var model = new Model(3, 2);
        var r = model.createReaction("some_reaction");
        var cytosol = model.createCompartment("c");

        r.setCompartment(cytosol);

        r.createReactant();
        r.createReactant();

        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(r);

        assertEquals(2, r.getListOfReactants().size());
        assertEquals(cytosol, r.getCompartmentInstance());
    }

    @Test
    public void sboParametersAreSet() {
        var model = model1507180049.getModel();

        Function<List<Reaction>, Integer> unsetSBOTermCount = (rs) ->
            Math.toIntExact(rs.stream().filter(Predicate.not(Reaction::isSetSBOTerm)).count());

        assertEquals(971, unsetSBOTermCount.apply(model.getListOfReactions()));

        new ReactionsPolisher(polishingParameters, sboParameters, new IdentifiersOrg()).polish(model.getListOfReactions());

        assertEquals(0, unsetSBOTermCount.apply(model.getListOfReactions()));
    }

}
