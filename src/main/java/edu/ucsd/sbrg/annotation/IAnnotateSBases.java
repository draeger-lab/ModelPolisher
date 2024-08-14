package edu.ucsd.sbrg.annotation;

import org.sbml.jsbml.SBase;

import java.sql.SQLException;
import java.util.List;

public interface IAnnotateSBases<SBMLElement extends SBase> {

    default void annotate(List<SBMLElement> elementsToAnnotate) throws SQLException, AnnotationException {
        for (var element: elementsToAnnotate) {
            annotate(element);
        }
    }

    void annotate(SBMLElement elementToAnnotate) throws SQLException, AnnotationException;

}
