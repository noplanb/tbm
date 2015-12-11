package com.zazoapp.client.ui.view;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Base class for filter TextWatcher
 * Created by skamenkovych@codeminders.com on 12/11/2015.
 */
public abstract class FilterWatcher implements TextWatcher {
    @Override
    public void afterTextChanged(Editable s) {
        onFilterChanged(s.toString());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    private void onFilterChanged(CharSequence text) {
        if (enoughToFilter(text)) {
            applyFilter(text);
        } else {
            applyFilter(null);
        }
    }

    /**
     * Filter is applied only when a minimum number of characters
     * was typed in the text view
     * @param text text
     * @see #enoughToFilter(CharSequence)
     */
    protected abstract void applyFilter(CharSequence text);

    /**
     *
     * @param text text
     * @return true if text satisfy the condition, otherwise false
     */
    protected boolean enoughToFilter(CharSequence text) {
        return text.length() >= 1;
    }
}
