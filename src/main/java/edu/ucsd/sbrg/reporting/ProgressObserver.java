package edu.ucsd.sbrg.reporting;

public interface ProgressObserver {

    void initialize(ProgressInitialization init);
    void update(ProgressUpdate update);
    void finish(ProgressFinalization finit);
}
