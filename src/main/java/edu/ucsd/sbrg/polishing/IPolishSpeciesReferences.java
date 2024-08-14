package edu.ucsd.sbrg.polishing;

import org.sbml.jsbml.SpeciesReference;

import java.util.List;

public interface IPolishSpeciesReferences {

    default void polish(List<SpeciesReference> elementsToPolish) {
        for (var element : elementsToPolish) {
            polish(element);
        }
    }

    void polish(SpeciesReference elementToPolish);

}
