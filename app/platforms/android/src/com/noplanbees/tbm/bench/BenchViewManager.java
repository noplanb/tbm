package com.noplanbees.tbm.bench;

/**
 * Created by skamenkovych@codeminders.com on 2/11/2015.
 */
public interface BenchViewManager {
    void showBench();
    void hideBench();
    boolean isBenchShowed();
    void updateBench();

    public interface Provider {
        BenchViewManager getBenchViewManager();
    }
}
