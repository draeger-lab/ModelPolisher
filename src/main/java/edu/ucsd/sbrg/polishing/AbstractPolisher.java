package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import org.sbml.jsbml.AbstractSBase;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPolisher<SBMLElement> {

    protected final Parameters parameters;

    private final List<ProgressObserver> observers;

    public AbstractPolisher(Parameters parameters) {
        this.parameters = parameters;
        observers = new ArrayList<>();
    }

    public AbstractPolisher(Parameters parameters, List<ProgressObserver> observers) {
        this.parameters = parameters;
        this.observers = observers;
    }

    public void polish(List<SBMLElement> elementsToPolish) {
        throw new UnsupportedOperationException();
    }

    abstract public void polish(SBMLElement elementToPolish);

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
