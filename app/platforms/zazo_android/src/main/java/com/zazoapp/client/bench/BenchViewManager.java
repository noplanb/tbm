package com.zazoapp.client.bench;

import android.view.View;

/**
 * Created by skamenkovych@codeminders.com on 2/11/2015.
 */
public interface BenchViewManager {
    void showBench();
    void hideBench();
    void setBenchShown(boolean isShown);
    boolean isBenchShown();
    void updateBench();
    void attachView(View view);
    void detachView();
}