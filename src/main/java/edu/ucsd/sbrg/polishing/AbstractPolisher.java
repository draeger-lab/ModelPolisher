package edu.ucsd.sbrg.polishing;

import edu.ucsd.sbrg.parameters.PolishingParameters;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import edu.ucsd.sbrg.reporting.ReportType;
import edu.ucsd.sbrg.resolver.Registry;

import java.util.*;

public abstract class AbstractPolisher<SBMLElement> {

    protected final PolishingParameters polishingParameters;
    protected final Registry registry;

    private final List<ProgressObserver> observers;

    public AbstractPolisher(PolishingParameters polishingParameters, Registry registry) {
        this.polishingParameters = polishingParameters;
        this.registry = registry;
        observers = new ArrayList<>();
    }

    public AbstractPolisher(PolishingParameters polishingParameters, Registry registry, List<ProgressObserver> observers) {
        this.polishingParameters = polishingParameters;
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

    protected void diffReport(String elementType, Object element1, Object element2) {
        for (var o : observers) {
            o.update(new ProgressUpdate(elementType, List.of(element1, element2), ReportType.DATA));
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
        return Objects.equals(polishingParameters, that.polishingParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(polishingParameters);
    }

    @Override
    public String toString() {
        return "AbstractPolisher{" +
                "parameters=" + polishingParameters +
                '}';
    }
}
