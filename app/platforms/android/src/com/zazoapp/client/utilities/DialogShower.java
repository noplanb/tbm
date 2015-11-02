package com.zazoapp.client.utilities;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Toast;
import com.zazoapp.client.R;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.multimedia.CameraException;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.ui.dialogs.*;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment.DoubleActionDialogListener;

/**
 * This utility class combines all popups which are showed to user
 * Created by skamenkovych@codeminders.com on 2/10/2015.
 */
public class DialogShower {

    private static final String FEATURE_FRAME = "featureFrame";

    private DialogShower() {};

    public static void showToast(final Context context, final String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    public static void showToast(Context context, int id) {
        showToast(context, context.getString(id));
    }

    public static void showBadConnection(Activity activity) {
        Resources res = activity.getResources();
        showActionInfoDialog(activity, res.getString(R.string.dialog_bad_connection_title),
                res.getString(R.string.dialog_bad_connection_message), res.getString(R.string.dialog_action_try_again),
                false, false, -1, null);
    }

    public static void showActionInfoDialog(Activity activity, String title, String message, String action,
                                            boolean needCancel, boolean editable, int actionId, AbstractDialogFragment.DialogListener listener) {
        DialogFragment d = ActionInfoDialogFragment.getInstance(title, message, action, actionId,
                needCancel, editable, listener);
        showDialog(activity, d, null);
    }

    public static void showDoubleActionDialog(Activity activity, String title, String message, String actionPositive,
                                              String actionNegative, int dialogId, boolean editable, DoubleActionDialogListener listener) {
        DialogFragment d = DoubleActionDialogFragment.getInstance(dialogId, title, message, actionPositive, actionNegative, editable, listener);
        showDialog(activity, d, null);
    }

    public static void showCameraException(final Activity activity, CameraException cameraException, final DoubleActionDialogListener listener, final int id) {
        Resources res = activity.getResources();
        final String title = res.getString(cameraException.getTitleId());
        final String message = res.getString(cameraException.getMessageId());
        final String positiveText = res.getString(R.string.dialog_action_try_again);
        final String negativeText = res.getString(R.string.dialog_action_quit);

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                DialogFragment d = DoubleActionDialogFragment.getInstance(id, title, message, positiveText,
                        negativeText, listener);
                showDialog(activity, d, null);
            }
        });
    }

    public static void showInfoDialog(Activity activity, String title, String message) {
        DialogFragment d = InfoDialogFragment.getInstance(title, message);
        showDialog(activity, d, null);
    }

    public static void showHintDialog(Activity activity, String title, String message) {
        if (activity.getFragmentManager().findFragmentByTag("hint") == null) {
            DialogFragment d = InfoDialogFragment.getInstance(title, message);
            showDialog(activity, d, "hint");
        }
    }

    public static void showVersionHandlerDialog(Activity activity, String message, boolean negativeButton) {
        if (activity.getFragmentManager().findFragmentByTag("compatibility") == null) {
            DialogFragment d = VersionDialogFragment.getInstance(message, negativeButton);
            showDialog(activity, d, "compatibility");
        }
    }

    public static void showSelectPhoneNumberDialog(Activity activity, Contact contact, SelectPhoneNumberDialog.Callbacks callbacks) {
        DialogFragment d = SelectPhoneNumberDialog.getInstance(contact, callbacks);
        showDialog(activity, d, null);
    }

    public static void showDialog(Activity activity, Fragment dialog, String tag) {
        FragmentManager manager = activity.getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(dialog, tag);
        transaction.commitAllowingStateLoss();
    }

    public static void showSendLinkDialog(Activity activity, int dialogId, String phone, String message, DoubleActionDialogListener callbacks) {
        DialogFragment d = SendLinkThroughDialog.getInstance(dialogId, phone, activity, message, callbacks);
        showDialog(activity, d, null);
    }

    public static void showFeatureAwardDialog(Activity activity, Features.Feature feature) {
        Fragment fragment = activity.getFragmentManager().findFragmentByTag(FEATURE_FRAME);
        if (fragment != null) {
            activity.getFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
        NotificationAlertManager.playTone(NotificationAlertManager.Tone.FEATURE_UNLOCK, NotificationAlertManager.getVelocity(activity, AudioManager.STREAM_NOTIFICATION));
        DialogFragment d = FeatureAwardDialogFragment.getInstance(feature);
        FragmentTransaction tr = activity.getFragmentManager().beginTransaction();
        tr.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
        tr.replace(R.id.feature_frame, d, FEATURE_FRAME);
        tr.commitAllowingStateLoss();
    }

    public static boolean isFeatureAwardDialogShown(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        if (fm != null) {
            Fragment fragment = fm.findFragmentByTag(FEATURE_FRAME);
            if (fragment instanceof FeatureAwardDialogFragment) {
                return ((FeatureAwardDialogFragment) fragment).isShown();
            }
        }
        return false;
    }

    public static void showNextFeatureDialog(Activity activity, boolean justUnlockedFeature) {
        Fragment fragment = activity.getFragmentManager().findFragmentByTag(FEATURE_FRAME);
        if (fragment != null) {
            activity.getFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
        FragmentTransaction tr = activity.getFragmentManager().beginTransaction();
        tr.setCustomAnimations(R.animator.slide_up, R.animator.slide_down);
        tr.replace(R.id.feature_frame, NextFeatureDialogFragment.getInstance(justUnlockedFeature), FEATURE_FRAME);
        tr.commitAllowingStateLoss();
    }

    public static void showBlockingDialog(Activity activity, int titleId, int messageId) {
        String title = activity.getString(titleId);
        String message = activity.getString(messageId);
        DialogFragment d = BlockingInfoDialog.getInstance(title, message);
        showDialog(activity, d, "blocking");
    }
}
