package de.uni_halle.informatik.biodata.mp.fixing;

import de.uni_halle.informatik.biodata.mp.reporting.IReportStatus;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressObserver;
import de.uni_halle.informatik.biodata.mp.reporting.ProgressUpdate;
import de.uni_halle.informatik.biodata.mp.reporting.ReportType;

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
