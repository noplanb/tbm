package com.zazoapp.client.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
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
import com.zazoapp.client.model.Avatar;
import com.zazoapp.client.model.AvatarProvidable;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.ui.view.CropImageView;
import com.zazoapp.client.utilities.AsyncTaskManager;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;
import de.hdodenhof.circleimageview.CircleImageView;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by skamenkovych@codeminders.com on 8/2/2016.
 */
public class AccountFragment extends ZazoTopFragment implements RadioGroup.OnCheckedChangeListener {

    private static final String USER_ID = "user_id";

    private static final String TAG = AccountFragment.class.getSimpleName();
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int PICK_IMAGE_REQUEST = 2;

    @InjectView(R.id.up) MaterialMenuView up;
    @InjectView(R.id.name) TextView name;
    @InjectView(R.id.thumb) CircleImageView thumb;
    @InjectView(R.id.thumb_cover) CircleImageView cover;
    @InjectView(R.id.edit_photo) TextView editPhoto;
    @InjectView(R.id.thumbnail_group) RadioGroup thumbnailChooserGroup;
    @InjectView(R.id.thumbnail_layout) View thumbnailLayout;
    @InjectView(R.id.use_last_frame) AppCompatRadioButton useLastFrameButton;
    @InjectView(R.id.use_profile_photo) AppCompatRadioButton useProfilePhotoButton;
    @InjectView(R.id.crop_view) CropImageView cropView;

    ThumbsHelper th;

    private DialogFragment pd;
    private String currentPhotoPath;
    private Bitmap lastBitmap;
    private BitmapDrawable lastAvatarPhoto;
    private CropScreen cropScreen;

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
            cover.setVisibility(View.VISIBLE);
            enableRadioGroup(true);
        } else {
            cover.setVisibility(View.INVISIBLE);
            enableRadioGroup(false);
        }
        Avatar.ThumbnailType type = user.getAvatar().getType();
        thumbnailChooserGroup.check(type == Avatar.ThumbnailType.LAST_FRAME ? R.id.use_last_frame : R.id.use_profile_photo);
        thumbnailChooserGroup.setOnCheckedChangeListener(this);
        up.setState(MaterialMenuDrawable.IconState.ARROW);
        cropScreen = new CropScreen(ButterKnife.findById(v, R.id.zazo_action_context_bar));
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
                        dispatchPickPhotoIntent();
                        break;
                    case 2:
                        final Avatar<User> avatar = user.getAvatar();
                        avatar.delete(true, new HttpRequest.Callbacks() {
                            @Override
                            public void success(String response) {
                                dismissProgressDialog();
                                if (thumb != null) {
                                    thumb.setImageResource(R.drawable.ic_account_circle_white);
                                    cover.setVisibility(View.INVISIBLE);
                                    new File(avatar.getAvatarPath()).delete();
                                }
                                avatar.delete(false, null);
                                user.set(AvatarProvidable.USE_AS_THUMBNAIL, Avatar.ThumbnailType.LAST_FRAME.optionName());
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

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                return;
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.zazoapp.client.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void dispatchPickPhotoIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.account_avatar_pick_photos)), PICK_IMAGE_REQUEST);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "zazo_avatar_photo";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".tmp",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            if (lastAvatarPhoto != null) {
                cropView.setImageDrawable(null);
                lastAvatarPhoto.getBitmap().recycle();
            }
            lastAvatarPhoto = new BitmapDrawable(getResources(), currentPhotoPath);
            loadPictureToCropScreen();
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), uri);
                if (lastAvatarPhoto != null) {
                    cropView.setImageDrawable(null);
                    lastAvatarPhoto.getBitmap().recycle();
                }
                lastAvatarPhoto = new BitmapDrawable(getResources(), bitmap);
                loadPictureToCropScreen();
            } catch (IOException e) {
            }
        }
    }

    private void loadPictureToCropScreen() {
        if (lastAvatarPhoto.getBitmap() == null) {
            DialogShower.showToast(getActivity(), R.string.account_avatar_cant_load);
            return;
        }
        final float w = lastAvatarPhoto.getBitmap().getWidth();
        final float h = lastAvatarPhoto.getBitmap().getHeight();

        showProgressDialog(R.string.account_avatar_searching_for_faces);
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Drawable, Void, RectF>() {
            @Override
            protected RectF doInBackground(Drawable... params) {
                FaceDetector detector = new FaceDetector((int) w, (int) h, 1);
                FaceDetector.Face[] faces = new FaceDetector.Face[1];
                lastBitmap = lastAvatarPhoto.getBitmap().copy(Bitmap.Config.RGB_565, false);
                int faceNumber = detector.findFaces(lastBitmap, faces);
                if (faceNumber == 1 && faces[0].confidence() >= 0.3f) {
                    float distance = faces[0].eyesDistance();
                    PointF midPoint = new PointF();
                    faces[0].getMidPoint(midPoint);
                    float scale = 1.7f;
                    RectF imageRect = new RectF(midPoint.x - distance * 3 / scale,
                            midPoint.y - distance * 3 / scale,
                            midPoint.x + distance * 3 / scale,
                            midPoint.y + distance * 5 / scale);
                    if (imageRect.right < w && imageRect.left >= 0 && imageRect.bottom < h && imageRect.top >= 0) {
                        Matrix matrix = new Matrix();
                        matrix.setScale(lastAvatarPhoto.getIntrinsicWidth() / w, lastAvatarPhoto.getIntrinsicHeight() / h);
                        matrix.mapRect(imageRect);
                        return imageRect;
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(RectF imageRect) {
                super.onPostExecute(imageRect);
                RectF rectCrop = getCropRectRelative();
                cropScreen.show();
                cropView.setImageDrawable(lastAvatarPhoto, rectCrop, imageRect);
                dismissProgressDialog();
            }
        });
    }

    @NonNull
    private RectF getCropRectRelative() {
        float horizontalPadding = 0.1f;
        float verticalPadding = (1f - (1f - 2 * horizontalPadding) * 4 / 3) / 2;
        return new RectF(horizontalPadding, verticalPadding, 1f - horizontalPadding, 1f - verticalPadding);
    }

    class CropScreen {
        @InjectView(R.id.title) TextView title;
        @InjectView(R.id.context_menu_view) MaterialMenuView menuView;
        private View rootView;

        CropScreen(View contextBar) {
            rootView = contextBar;
            View.inflate(contextBar.getContext(), R.layout.crop_screen_action_bar, (ViewGroup) contextBar);
            ButterKnife.inject(this, contextBar);
            menuView.setState(MaterialMenuDrawable.IconState.X);
        }

        @OnClick(R.id.context_menu_view)
        public void onMenuClicked(View v) {
            onContextMenuClosed();
        }

        @OnClick(R.id.done_btn)
        public void onDoneClicked(View v) {
            onAvatarSet();
        }

        public void show() {
            doAppearing();
        }

        private void doAppearing() {
            rootView.animate().alpha(1f).setListener(null).start();
            rootView.setVisibility(View.VISIBLE);
            cropView.animate().alpha(1f).setListener(null).start();
            cropView.setVisibility(View.VISIBLE);
        }

        public void hide() {
            rootView.animate().alpha(0f).setDuration(400).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    rootView.setVisibility(View.INVISIBLE);
                }
            }).start();
            cropView.animate().alpha(0f).setDuration(400).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    cropView.setVisibility(View.INVISIBLE);
                }
            }).start();
        }
    }

    private void onAvatarSet() {
        RectF cropRect = cropView.getCroppedImageRect();
        Drawable drawable = cropView.getDrawable();

        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Matrix matrix = new Matrix();
            matrix.setScale(bitmap.getWidth() / (float) drawable.getIntrinsicWidth(), bitmap.getHeight() / (float) drawable.getIntrinsicHeight());
            matrix.mapRect(cropRect);
            Bitmap avatarBitmap = Bitmap.createBitmap(bitmap, (int) cropRect.left, (int) cropRect.top, (int) cropRect.width(), (int) cropRect.height());
            thumb.setImageBitmap(avatarBitmap);
            cover.setVisibility(View.VISIBLE);
            User user = UserFactory.getFactoryInstance().find(getArguments().getString(USER_ID));
            if (user != null) {
                FileOutputStream fos = null;
                try {
                    fos = FileUtils.openOutputStream(new File(user.getAvatar().getAvatarPath()));
                    avatarBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                } catch (IOException e) {
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {}
                    }
                }
                user.getAvatar().updateBitmap();
                if (user.getAvatar().exists()) {
                    Avatar.upload(user.getAvatar().getAvatarPath(),
                            thumbnailChooserGroup.getCheckedRadioButtonId() == R.id.use_profile_photo ? Avatar.ThumbnailType.PHOTO : Avatar.ThumbnailType.LAST_FRAME);
                    enableRadioGroup(true);
                } else {
                    enableRadioGroup(false);
                }
            }
        }
        cropScreen.hide();
    }

    private void onContextMenuClosed() {
        // TODO dismiss crop screen
        cropScreen.hide();
    }
}
