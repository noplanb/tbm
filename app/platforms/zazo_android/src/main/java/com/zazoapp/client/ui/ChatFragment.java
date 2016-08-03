package com.zazoapp.client.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.ui.view.CircleThumbView;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

import java.io.File;

/**
 * Created by skamenkovych@codeminders.com on 8/2/2016.
 */
public class ChatFragment extends ZazoTopFragment {

    private static final String NAME = "name";
    private static final String THUMB_PATH = "thumb_path";
    private static final String FRIEND_ID = "friend_id";
    private static final String TEXT = "text";

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.action_bar_icon) CircleThumbView thumb;
    @InjectView(R.id.action_bar_title) TextView title;
    @InjectView(R.id.send) View send;
    @InjectView(R.id.texter) TextView texter;

    ThumbsHelper th;
    private int previousSoftInputMode;

    public static ChatFragment getInstance(Friend friend) {
        ChatFragment f = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(NAME, friend.getFirstName());
        args.putString(THUMB_PATH, Friend.File.IN_THUMB.getPath(friend, friend.getId()));
        args.putString(FRIEND_ID, friend.getId());
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.chat_layout, null);
        ButterKnife.inject(this, v);
        th = new ThumbsHelper(v.getContext());
        texter.setText(getArguments().getString(TEXT));
        String name = getArguments().getString(NAME);
        title.setText(name);
        File file = new File(getArguments().getString(THUMB_PATH));
        if (file.exists()) {
            thumb.setImageBitmap(Convenience.bitmapWithFile(file));
            thumb.setFillColor(Color.TRANSPARENT);
        } else {
            thumb.setImageResource(th.getIcon(name));
            thumb.setFillColor(th.getColor(name));
        }
        up.setState(MaterialMenuDrawable.IconState.X);
        send.setEnabled(!TextUtils.isEmpty(texter.getText()));
        texter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                send.setEnabled(s.length() > 0);
            }
        });
        return v;
    }

    @OnClick(R.id.send)
    public void onSend(View v) {
        DialogShower.showToast(v.getContext(), "onSend to" + getArguments().getString(NAME) + " " + getArguments().getString(FRIEND_ID));
        texter.clearComposingText();
        dismiss();
    }

    @OnClick(R.id.up)
    public void onClose(View v) {
        dismiss();
    }

    private void dismiss() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getWindow().setSoftInputMode(previousSoftInputMode);
        }
        InputMethodManager imm = (InputMethodManager) texter.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(texter.getWindowToken(), 0);
        }
        super.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    }

    @Override
    protected void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Window window = activity.getWindow();

    }

    @Override
    public void onPause() {
        super.onPause();
        getArguments().putString(TEXT, String.valueOf(texter.getText()));
    }

    @Override
    public void onResume() {
        super.onResume();
        texter.setText(getArguments().getString(TEXT));
        Window window = getActivity().getWindow();
        previousSoftInputMode = window.getAttributes().softInputMode;
        window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);
    }
}
