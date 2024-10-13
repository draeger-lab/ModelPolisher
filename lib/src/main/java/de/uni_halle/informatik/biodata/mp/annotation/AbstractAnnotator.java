package de.uni_halle.informatik.biodata.mp.annotation;

import de.uni_halle.informatik.biodata.mp.reporting.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAnnotator implements IReportStatus, IReportDiffs {

    private final List<ProgressObserver> observers;

    public AbstractAnnotator() {
        observers = new ArrayList<>();
    }

    public AbstractAnnotator(List<ProgressObserver> observers) {
        this.observers = observers;
    }

    @Override
    public void statusReport(String text, Object element) {
        for (var o : observers) {
            o.update(new ProgressUpdate(text, element, ReportType.STATUS));
        }
    }

    @Override
    public void diffReport(String elementType, Object element1, Object element2) {
        for (var o : observers) {
            o.update(new ProgressUpdate(elementType, List.of(element1, element2), ReportType.DATA));
        }
    }
    public List<ProgressObserver> getObservers() {
        return observers;
    }

}
