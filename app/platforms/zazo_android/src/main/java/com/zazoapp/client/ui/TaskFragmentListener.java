package com.zazoapp.client.ui;

import android.content.Intent;

/**
 * Created by skamenkovych@codeminders.com on 11/10/2015.
 */
public interface TaskFragmentListener {
    void onFragmentResult(int requestCode, int resultCode, Intent data);
}
