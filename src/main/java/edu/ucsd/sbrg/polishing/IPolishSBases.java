package edu.ucsd.sbrg.polishing;

import org.sbml.jsbml.SBase;

import java.util.List;

public interface IPolishSBases<SBMLElement extends SBase> {

    default void polish(List<SBMLElement> elementsToPolish) {
        for (var element : elementsToPolish) {
            polish(element);
        }
    }

    void polish(SBMLElement elementToPolish);

}
