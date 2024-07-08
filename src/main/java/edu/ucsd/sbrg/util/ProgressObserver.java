package edu.ucsd.sbrg.util;

public interface ProgressObserver {

    void initialize(ProgressInitialization init);
    void update(ProgressUpdate update);
    void finish(ProgressFinalization finit);
}
