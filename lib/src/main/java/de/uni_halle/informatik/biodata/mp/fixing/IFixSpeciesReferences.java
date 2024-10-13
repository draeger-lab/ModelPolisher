package de.uni_halle.informatik.biodata.mp.fixing;

import org.sbml.jsbml.SpeciesReference;

import java.util.List;

public interface IFixSpeciesReferences {

    default void fix(List<SpeciesReference> elements) {
        for (var sr : elements) {
            fix(sr);
        }
    }

    void fix(SpeciesReference element);
}
