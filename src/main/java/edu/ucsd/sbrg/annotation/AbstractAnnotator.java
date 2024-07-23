package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import edu.ucsd.sbrg.reporting.ReportType;

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

    public void annotate(List<SBMLElement> elementsToAnnotate) {
        throw new UnsupportedOperationException();
    }

    abstract public void annotate(SBMLElement elementToAnnotate);

    protected void statusReport(String text, Object element) {
        for (var o : observers) {
            o.update(new ProgressUpdate(text, element, ReportType.STATUS));
        }
    }

    public List<ProgressObserver> getObservers() {
        return observers;
    }

}
