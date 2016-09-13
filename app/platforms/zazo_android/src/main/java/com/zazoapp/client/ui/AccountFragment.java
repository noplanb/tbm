package com.zazoapp.client.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.ListPopupWindow;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
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
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;
import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by skamenkovych@codeminders.com on 8/2/2016.
 */
public class AccountFragment extends ZazoTopFragment {

    private static final String USER_ID = "user_id";

    private static final String TAG = AccountFragment.class.getSimpleName();

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.name) TextView name;
    @InjectView(R.id.thumb) CircleImageView thumb;
    @InjectView(R.id.edit_photo) TextView editPhoto;
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
        name.setText(user.getFullName());
        name.setTypeface(Convenience.getTypeface(v.getContext()));
        editPhoto.setTypeface(Convenience.getTypeface(v.getContext(), Convenience.NORMAL));
        if (user.getAvatar().exists()) {
            thumb.setImageBitmap(user.getAvatar().loadBitmap());
        }
        //File file = new File(getArguments().getString(THUMB_PATH));
        //if (file.exists()) {
        //    thumb.setImageBitmap(Convenience.bitmapWithFile(file));
        //    thumb.setFillColor(Color.TRANSPARENT);
        //} else {
        //    thumb.setImageResource(th.getIcon(name));
        //    thumb.setFillColor(th.getColor(name));
        //}
        up.setState(MaterialMenuDrawable.IconState.ARROW);
        return v;
    }

    @OnClick(R.id.up)
    public void onClose(View v) {
        dismiss();
    }

    @OnClick(R.id.edit_photo)
    public void onEditPhoto(View v) {
        final Context c = v.getContext();
        final String[] options = c.getResources().getStringArray(R.array.account_photo_options);
        String[] from = new String[] {"text"};
        final ListPopupWindow listPopupWindow = new ListPopupWindow(c);
        ArrayList<Map<String, String>> list = new ArrayList<>(options.length);
        for (String option : options) {
            Map<String, String> map = new HashMap<>();
            map.put("text", option);
            list.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(c, list, android.R.layout.simple_list_item_1, from, new int[]{android.R.id.text1});
        listPopupWindow.setAdapter(adapter);
        listPopupWindow.setDropDownGravity(Gravity.START);
        listPopupWindow.setListSelector(getResources().getDrawable(R.drawable.options_popup_item_bg));
        listPopupWindow.setAnchorView(editPhoto);
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int phoneIndex, long id) {
                //finishAction.onPhoneItemSelected(phoneIndex);
                DialogShower.showToast(c, "Test " + options[phoneIndex]);
                listPopupWindow.dismiss();
            }
        });
        listPopupWindow.show();
    }

    private void dismiss() {
        super.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    }

}
