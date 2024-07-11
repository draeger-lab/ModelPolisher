package edu.ucsd.sbrg.reporting;

import de.zbit.util.progressbar.ProgressBar;

public class PolisherProgressBar implements ProgressObserver {

    ProgressBar bar;

    @Override
    public void initialize(ProgressInitialization init) {
        bar = new ProgressBar(init.totalCalls());
    }

    @Override
    public void update(ProgressUpdate update) {
        bar.DisplayBar(update.text());
    }

    @Override
    public void finish(ProgressFinalization finit) {
        bar.finished();
    }
}
