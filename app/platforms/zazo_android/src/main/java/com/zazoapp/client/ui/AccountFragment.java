package com.zazoapp.client.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.ui.view.CircleThumbView;

/**
 * Created by skamenkovych@codeminders.com on 8/2/2016.
 */
public class AccountFragment extends ZazoTopFragment {

    private static final String USER_ID = "user_id";

    private static final String TAG = AccountFragment.class.getSimpleName();

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.action_bar_title) TextView title;
    @InjectView(R.id.thumb) CircleThumbView thumb;
    @InjectView(R.id.edit_photo) View editPhoto;
    @InjectView(R.id.thumbnail_group) RadioGroup thumbnailChooserGroup;
    @InjectView(R.id.use_last_frame) AppCompatRadioButton useLastFrameButton;
    @InjectView(R.id.use_profile_photo) AppCompatRadioButton useProfilePhotoButton;

    ThumbsHelper th;

    public static AccountFragment getInstance() {
        AccountFragment f = new AccountFragment();
        User user = UserFactory.current_user();
        Bundle args = new Bundle();
        if (user != null) {
            args.putString(USER_ID, user.getId());
        }
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.account_layout, null);
        ButterKnife.inject(this, v);
        User user = UserFactory.getFactoryInstance().find(getArguments().getString(USER_ID));
        if (user == null) {
            return v;
        }
        th = new ThumbsHelper(v.getContext());
        title.setText(user.getFullName());
        //File file = new File(getArguments().getString(THUMB_PATH));
        //if (file.exists()) {
        //    thumb.setImageBitmap(Convenience.bitmapWithFile(file));
        //    thumb.setFillColor(Color.TRANSPARENT);
        //} else {
        //    thumb.setImageResource(th.getIcon(name));
        //    thumb.setFillColor(th.getColor(name));
        //}
        up.setState(MaterialMenuDrawable.IconState.X);
        return v;
    }

    @OnClick(R.id.up)
    public void onClose(View v) {
        dismiss();
    }

    private void dismiss() {
        super.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    }

}
