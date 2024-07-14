package edu.ucsd.sbrg.annotation;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import org.sbml.jsbml.AbstractSBase;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAnnotator<SBMLElement> {

    protected final Parameters parameters;

    private final List<ProgressObserver> observers;

    public AbstractAnnotator(Parameters parameters) {
        this.parameters = parameters;
        observers = new ArrayList<>();
    }

    public AbstractAnnotator(Parameters parameters, List<ProgressObserver> observers) {
        this.parameters = parameters;
        this.observers = observers;
    }

    public void annotate(List<SBMLElement> elementsToAnnotate) {
        throw new UnsupportedOperationException();
    }

    abstract public void annotate(SBMLElement elementToAnnotate);

    protected void updateProgressObservers(String text, AbstractSBase obj) {
        for (var o : observers) {
            o.update(new ProgressUpdate(text, obj));
        }
    }

    public void addObserver(ProgressObserver o) {
        observers.add(o);
    }

    public List<ProgressObserver> getObservers() {
        return observers;
    }
}
