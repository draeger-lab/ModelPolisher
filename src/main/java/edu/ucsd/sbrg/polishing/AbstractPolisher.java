package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.Parameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import edu.ucsd.sbrg.reporting.ReportType;
import edu.ucsd.sbrg.resolver.Registry;

import java.util.*;

public abstract class AbstractPolisher<SBMLElement> {

    protected final Parameters parameters;
    protected final Registry registry;

    private final List<ProgressObserver> observers;

    public AbstractPolisher(Parameters parameters, Registry registry) {
        this.parameters = parameters;
        this.registry = registry;
        observers = new ArrayList<>();
    }

    public AbstractPolisher(Parameters parameters, Registry registry, List<ProgressObserver> observers) {
        this.parameters = parameters;
        this.registry = registry;
        this.observers = observers;
    }

    public void polish(List<SBMLElement> elementsToPolish) {
        throw new UnsupportedOperationException();
    }

    abstract public void polish(SBMLElement elementToPolish);

    protected void statusReport(String text, Object element) {
        for (var o : observers) {
            o.update(new ProgressUpdate(text, element, ReportType.STATUS));
        }
    }

    public List<ProgressObserver> getObservers() {
        return observers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractPolisher<?> that = (AbstractPolisher<?>) o;
        return Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameters);
    }

    @Override
    public String toString() {
        return "AbstractPolisher{" +
                "parameters=" + parameters +
                '}';
    }
}
