package edu.ucsd.sbrg.fixing;

import org.sbml.jsbml.SBase;

import java.util.List;

public interface IFixSBases<SBMLElement extends SBase> {

    default void fix(List<SBMLElement> elements) {
        var iterator = elements.listIterator();
        while (iterator.hasNext()) {
            fix(iterator.next(), iterator.nextIndex());
        }
    }

    void fix(SBMLElement element, int index);
}
