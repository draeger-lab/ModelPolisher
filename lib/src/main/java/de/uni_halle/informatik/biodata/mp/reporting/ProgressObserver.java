package de.uni_halle.informatik.biodata.mp.reporting;

public interface ProgressObserver {

    void initialize(ProgressInitialization init);
    void update(ProgressUpdate update);
    void finish(ProgressFinalization finit);
}
