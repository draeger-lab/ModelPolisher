package edu.ucsd.sbrg.fixing;

import edu.ucsd.sbrg.reporting.IReportStatus;
import edu.ucsd.sbrg.reporting.ProgressObserver;
import edu.ucsd.sbrg.reporting.ProgressUpdate;
import edu.ucsd.sbrg.reporting.ReportType;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFixer implements IReportStatus {

    private final List<ProgressObserver> observers;

    protected AbstractFixer(List<ProgressObserver> observers) {
        this.observers = observers;
    }

    public AbstractFixer() {
        observers = new ArrayList<>();
    }

    @Override
    public void statusReport(String text, Object element) {
        for (var o : observers) {
            o.update(new ProgressUpdate(text, element, ReportType.STATUS));
        }
    }

    public List<ProgressObserver> getObservers() {
        return observers;
    }

}
