package com.zazoapp.client.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
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
import com.zazoapp.client.model.Avatar;
import com.zazoapp.client.model.AvatarProvidable;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
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
public class AccountFragment extends ZazoTopFragment implements RadioGroup.OnCheckedChangeListener {

    private static final String USER_ID = "user_id";

    private static final String TAG = AccountFragment.class.getSimpleName();

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.name) TextView name;
    @InjectView(R.id.thumb) CircleImageView thumb;
    @InjectView(R.id.edit_photo) TextView editPhoto;
    @InjectView(R.id.thumbnail_group) RadioGroup thumbnailChooserGroup;
    @InjectView(R.id.thumbnail_layout) View thumbnailLayout;
    @InjectView(R.id.use_last_frame) AppCompatRadioButton useLastFrameButton;
    @InjectView(R.id.use_profile_photo) AppCompatRadioButton useProfilePhotoButton;

    ThumbsHelper th;

    private DialogFragment pd;

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
            enableRadioGroup(true);
        } else {
            enableRadioGroup(false);
        }
        Avatar.ThumbnailType type = user.getAvatar().getType();
        thumbnailChooserGroup.check(type == Avatar.ThumbnailType.LAST_FRAME ? R.id.use_last_frame : R.id.use_profile_photo);
        thumbnailChooserGroup.setOnCheckedChangeListener(this);
        up.setState(MaterialMenuDrawable.IconState.ARROW);
        return v;
    }

    @OnClick(R.id.up)
    public void onClose(View v) {
        dismiss();
    }

    @OnClick(R.id.edit_photo)
    public void onEditPhoto(View v) {
        final User user = UserFactory.getFactoryInstance().find(getArguments().getString(USER_ID));
        if (user == null) {
            return;
        }
        final Context c = v.getContext();
        final String[] options = c.getResources().getStringArray(R.array.account_photo_options);
        String[] from = new String[] {"text"};
        final ListPopupWindow listPopupWindow = new ListPopupWindow(c);
        ArrayList<Map<String, String>> list = new ArrayList<>(options.length);
        boolean showDeleteOption = user.getAvatar().existsSomewhere();
        for (int i = 0; i < (options.length - (showDeleteOption ? 0 : 1)); i++) {
            Map<String, String> map = new HashMap<>();
            map.put("text", options[i]);
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
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        // Take screenshot
                        dispatchTakePictureIntent();
                        break;
                    case 1:
                        // choose file
                        break;
                    case 2:
                        final Avatar<User> avatar = user.getAvatar();
                        avatar.delete(true, new HttpRequest.Callbacks() {
                            @Override
                            public void success(String response) {
                                dismissProgressDialog();
                                if (thumb != null) {
                                    thumb.setImageResource(R.drawable.ic_account_circle_white);
                                }
                                avatar.delete(false, null);
                                thumbnailChooserGroup.check(R.id.use_last_frame);
                                enableRadioGroup(false);
                            }

                            @Override
                            public void error(String errorString) {
                                dismissProgressDialog();
                            }
                        });
                        showProgressDialog(R.string.dialog_deleting_title);
                        break;
                }
                listPopupWindow.dismiss();

            }
        });
        listPopupWindow.show();
    }

    private void dismiss() {
        super.onKeyDown(KeyEvent.KEYCODE_BACK, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
    }

    private void showProgressDialog(@StringRes int title) {
        dismissProgressDialog();
        pd = ProgressDialogFragment.getInstance(null, getString(title));
        DialogShower.showDialog(getChildFragmentManager(), pd, null);
    }

    private void dismissProgressDialog() {
        if (pd != null)
            pd.dismissAllowingStateLoss();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        final User user = UserFactory.getFactoryInstance().find(getArguments().getString(USER_ID));
        if (user == null) {
            return;
        }
        Avatar.ThumbnailType type = null;
        switch (checkedId) {
            case R.id.use_last_frame:
                type = Avatar.ThumbnailType.LAST_FRAME;
                break;
            case R.id.use_profile_photo:
                type = Avatar.ThumbnailType.PHOTO;
                break;
        }
        if (type != null && type != user.getAvatar().getType()) {
            final Avatar.ThumbnailType finalType = type;
            Avatar.update(type, new HttpRequest.Callbacks() {
                @Override
                public void success(String response) {
                    dismissProgressDialog();
                    user.set(AvatarProvidable.USE_AS_THUMBNAIL, finalType.optionName());
                }

                @Override
                public void error(String errorString) {
                    dismissProgressDialog();
                    thumbnailChooserGroup.check(user.getAvatar().getType() == Avatar.ThumbnailType.LAST_FRAME ? R.id.use_last_frame : R.id.use_profile_photo);
                }
            });
            showProgressDialog(R.string.dialog_syncing_title);
        }
    }
    
    private void enableRadioGroup(boolean enable) {
        thumbnailLayout.setAlpha(enable ? 1f : 0.72f);
        for (int i = 0; i < thumbnailChooserGroup.getChildCount(); i++) {
            View child = thumbnailChooserGroup.getChildAt(i);
            child.setEnabled(enable);
        }
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "result " + (resultCode == Activity.RESULT_OK));
        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
        // TODO temporary
        thumb.setImageBitmap(bitmap);
    }
}
