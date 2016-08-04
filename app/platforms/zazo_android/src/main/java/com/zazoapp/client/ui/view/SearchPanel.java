package com.zazoapp.client.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.view.View;
import android.widget.EditText;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.helpers.UiUtils;

/**
 * Created by skamenkovych@codeminders.com on 12/11/2015.
 */
public class SearchPanel {
    @InjectView(R.id.search_view) EditText searchView;
    @InjectView(R.id.search_layout) View searchLayout;

    public SearchPanel(View parentView) {
        ButterKnife.inject(this, parentView);
    }

    public void addTextChangedListener(TextWatcher watcher) {
        searchView.addTextChangedListener(watcher);
    }

    public CharSequence getText() {
        return searchView.getText();
    }

    public void clearTextView() {
        if (searchView == null || TextUtils.isEmpty(searchView.getText()))
            return;
        TextKeyListener.clear(searchView.getEditableText());
    }

    public void hideKeyboard() {
        UiUtils.hideSoftKeyboard(searchView);
    }

    public void showKeyboard() {
        UiUtils.showSoftKeyboard(searchView);
    }

    @OnClick({R.id.search_button, R.id.search_action_view})
    public void onSearchButtonClicked(View v) {
        searchLayout.setVisibility(View.VISIBLE);
        searchLayout.animate().setListener(null).alpha(1f).start();
        searchView.requestFocusFromTouch();
        showKeyboard();
    }

    @OnClick(R.id.search_back)
    public void onSearchBackButtonClicked(View v) {
        closeSearch();
    }

    public void closeSearch() {
        searchLayout.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                searchLayout.setVisibility(View.INVISIBLE);
                clearTextView();
            }
        }).start();
        hideKeyboard();
    }

    @OnClick(R.id.search_clear)
    public void onSearchClearButtonClicked(View v) {
        clearTextView();
    }
}
