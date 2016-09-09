package com.zazoapp.client.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.core.MessageType;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.model.Avatar;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.multimedia.VideoIdUtils;
import com.zazoapp.client.network.NetworkConfig;
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.ui.helpers.UiUtils;
import com.zazoapp.client.ui.view.AutoResizeEditText;
import com.zazoapp.client.ui.view.CircleThumbView;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by skamenkovych@codeminders.com on 8/2/2016.
 */
public class ChatFragment extends ZazoTopFragment {

    private static final String NAME = "name";
    private static final String THUMB_PATH = "thumb_path";
    private static final String FRIEND_ID = "friend_id";
    private static final String TEXT = "text";

    private static final String TAG = ChatFragment.class.getSimpleName();

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.action_bar_icon) CircleThumbView thumb;
    @InjectView(R.id.action_bar_title) TextView title;
    @InjectView(R.id.send) View send;
    @InjectView(R.id.texter) AutoResizeEditText texter;

    ThumbsHelper th;
    private int previousSoftInputMode;
    private boolean taskCompleted;
    private Friend friend;

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
        friend = FriendFactory.getFactoryInstance().find(getArguments().getString(FRIEND_ID));
        th = new ThumbsHelper(v.getContext());
        texter.setText(getArguments().getString(TEXT));
        String name = getArguments().getString(NAME);
        title.setText(name);
        if (friend != null) {
            Avatar<Friend> avatar = friend.getAvatar();
            if (avatar.existsSomewhere()) {
                avatar.loadTo(thumb);
                thumb.setFillColor(Color.TRANSPARENT);
            } else {
                thumb.setImageResource(th.getIcon(name));
                thumb.setFillColor(th.getColor(name));
            }
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
        String messageText = String.valueOf(texter.getText());
        if (acceptableText(messageText)) {
            if (friend != null) {
                String messageId = VideoIdUtils.generateId();
                String path = Friend.File.OUT_TEXT.getPath(friend, messageId);
                Convenience.saveTextToFile(messageText, path);
                friend.setNewOutgoingMessage(messageId, MessageType.TEXT);
                friend.requestUpload(messageId);
                if (!NetworkConfig.isConnected(v.getContext())) {
                    DialogShower.showToast(v.getContext(), R.string.toast_no_connection);
                }
                ZazoManagerProvider managers = TbmApplication.getInstance().getManagerProvider();
                if (managers != null) {
                    managers.getTutorial().onMessageWritten(friend);
                }
            }
            TextKeyListener.clear(texter.getEditableText());

            taskCompleted = true;
            dismiss();
        } else {
            TextKeyListener.clear(texter.getEditableText());
            DialogShower.showToast(v.getContext(), R.string.toast_input_is_empty);
        }
    }

    @OnClick(R.id.up)
    public void onClose(View v) {
        dismiss();
    }

    private void dismiss() {
        super.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    }

    @Override
    public void onPause() {
        super.onPause();
        UiUtils.hideSoftKeyboard(texter);
        Window window = getActivity().getWindow();
        window.setSoftInputMode(previousSoftInputMode);
    }

    @Override
    public void onResume() {
        super.onResume();
        texter.requestFocusFromTouch();
        Window window = getActivity().getWindow();
        previousSoftInputMode = window.getAttributes().softInputMode;
        window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED);

        texter.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isResumed()) {
                    UiUtils.showSoftKeyboard(texter);
                }
            }
        }, 300);
    }

    public boolean isTaskCompleted() {
        return taskCompleted;
    }

    private static void main(String[] args) {
        LinkedHashMap<String, Boolean> texts = new LinkedHashMap<>();
        texts.put("", false);
        texts.put(" ", false);
        texts.put("\n", false);
        texts.put(" \n\t", false);
        texts.put("Text", true);
        texts.put("Text with spaces", true);
        texts.put("Text with multiple symbols   -\n    \t asdasd", true);
        for (Map.Entry<String, Boolean> entry : texts.entrySet()) {
            System.out.println("Test " +
                    (entry.getValue().equals(acceptableText(entry.getKey())) ? "Passed" : "Failed"));
        }
    }

    private static boolean acceptableText(String text) {
        Pattern pattern = Pattern.compile("[\\S]+");
        Matcher m = pattern.matcher(text);
        return m.find();
    }
}
