package edu.ucsd.sbrg.polishing;

import org.sbml.jsbml.SBase;

import java.util.List;

public interface IPolishSBaseAttributes {


    default void polish(List<SBase> elementsToPolish) {
        for (var element : elementsToPolish) {
            polish(element);
        }
    }

    void polish(SBase elementToPolish);


}
