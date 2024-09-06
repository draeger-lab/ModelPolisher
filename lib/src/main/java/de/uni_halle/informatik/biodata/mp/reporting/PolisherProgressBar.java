package de.uni_halle.informatik.biodata.mp.reporting;

import de.zbit.util.progressbar.ProgressBar;

public class PolisherProgressBar implements ProgressObserver {

    ProgressBar bar;

    @Override
    public void initialize(ProgressInitialization init) {
        bar = new ProgressBar(init.totalCalls());
    }

    @Override
    public void update(ProgressUpdate update) {
        if(update.reportType().equals(ReportType.STATUS))
            bar.DisplayBar(update.text());
    }

    @Override
    public void finish(ProgressFinalization finit) {
        bar.DisplayBar(finit.message());
        bar.finished();
    }
}
