package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import edu.ucsd.sbrg.reporting.ReportType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAnnotator<SBMLElement> {

    private final List<ProgressObserver> observers;

    public AbstractAnnotator() {
        observers = new ArrayList<>();
    }

    public AbstractAnnotator(List<ProgressObserver> observers) {
        this.observers = observers;
    }

    public void annotate(List<SBMLElement> elementsToAnnotate) throws SQLException {
        throw new UnsupportedOperationException();
    }

    abstract public void annotate(SBMLElement elementToAnnotate) throws SQLException, AnnotationException;

    protected void statusReport(String text, Object element) {
        for (var o : observers) {
            o.update(new ProgressUpdate(text, element, ReportType.STATUS));
        }
    }

    protected void diffReport(String elementType, Object element1, Object element2) {
        for (var o : observers) {
            o.update(new ProgressUpdate(elementType, List.of(element1, element2), ReportType.DATA));
        }
    }
    public List<ProgressObserver> getObservers() {
        return observers;
    }

}
