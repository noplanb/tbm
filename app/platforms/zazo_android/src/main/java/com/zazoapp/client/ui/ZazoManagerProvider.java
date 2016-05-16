package com.zazoapp.client.ui;

import com.zazoapp.client.bench.BenchViewManager;
import com.zazoapp.client.tutorial.Tutorial;

/**
 * Created by skamenkovych@codeminders.com on 4/21/2015.
 */
public interface ZazoManagerProvider extends BaseManagerProvider {
    BenchViewManager getBenchViewManager();
    Tutorial getTutorial();
}
