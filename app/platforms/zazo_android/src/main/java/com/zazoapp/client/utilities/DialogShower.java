package com.zazoapp.client.utilities;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.widget.Toast;
import com.zazoapp.client.R;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.multimedia.CameraException;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.ui.dialogs.AbstractDialogFragment;
import com.zazoapp.client.ui.dialogs.ActionInfoDialogFragment;
import com.zazoapp.client.ui.dialogs.BlockingInfoDialog;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment.DoubleActionDialogListener;
import com.zazoapp.client.ui.dialogs.FeatureAwardDialogFragment;
import com.zazoapp.client.ui.dialogs.InfoDialogFragment;
import com.zazoapp.client.ui.dialogs.NextFeatureDialogFragment;
import com.zazoapp.client.ui.dialogs.SelectPhoneNumberDialog;
import com.zazoapp.client.ui.dialogs.SendLinkThroughDialog;
import com.zazoapp.client.ui.dialogs.VersionDialogFragment;

/**
 * This utility class combines all popups which are showed to user
 * Created by skamenkovych@codeminders.com on 2/10/2015.
 */
public class DialogShower {

    private static final String FEATURE_FRAME = "featureFrame";

    private DialogShower() {}

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

    public static void showBadConnection(FragmentActivity activity) {
        Resources res = activity.getResources();
        showActionInfoDialog(activity, res.getString(R.string.dialog_bad_connection_title),
                res.getString(R.string.dialog_bad_connection_message), res.getString(R.string.dialog_action_try_again),
                false, false, -1, null);
    }

    public static void showActionInfoDialog(FragmentActivity activity, String title, String message, String action,
                                            boolean needCancel, boolean editable, int actionId, AbstractDialogFragment.DialogListener listener) {
        DialogFragment d = ActionInfoDialogFragment.getInstance(title, message, action, actionId,
                needCancel, editable, listener);
        showDialog(getFragmentManager(activity, listener), d, null);
    }

    public static void showDoubleActionDialog(FragmentActivity activity, String title, String message, String actionPositive,
                                              String actionNegative, int dialogId, boolean editable, DoubleActionDialogListener listener) {
        DialogFragment d = DoubleActionDialogFragment.getInstance(dialogId, title, message, actionPositive, actionNegative, editable, listener);
        showDialog(getFragmentManager(activity, listener), d, null);
    }

    public static void showCameraException(final FragmentActivity activity, CameraException cameraException, final DoubleActionDialogListener listener, final int id) {
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
                showDialog(getFragmentManager(activity, listener), d, null);
            }
        });
    }

    public static void showInfoDialog(FragmentActivity activity, String title, String message) {
        DialogFragment d = InfoDialogFragment.getInstance(title, message);
        showDialog(activity.getSupportFragmentManager(), d, null);
    }

    public static void showHintDialog(FragmentActivity activity, String title, String message) {
        if (activity.getSupportFragmentManager().findFragmentByTag("hint") == null) {
            DialogFragment d = InfoDialogFragment.getInstance(title, message);
            showDialog(activity.getSupportFragmentManager(), d, "hint");
        }
    }

    public static void showVersionHandlerDialog(FragmentActivity activity, String message, boolean negativeButton) {
        if (activity.getSupportFragmentManager().findFragmentByTag("compatibility") == null) {
            DialogFragment d = VersionDialogFragment.getInstance(message, negativeButton);
            showDialog(activity.getSupportFragmentManager(), d, "compatibility");
        }
    }

    public static void showSelectPhoneNumberDialog(FragmentActivity activity, Contact contact, SelectPhoneNumberDialog.Callbacks callbacks) {
        DialogFragment d = SelectPhoneNumberDialog.getInstance(contact, callbacks);
        showDialog(getFragmentManager(activity, callbacks), d, null);
    }

    public static void showDialog(FragmentManager fragmentManager, Fragment dialog, String tag) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(dialog, tag);
        transaction.commitAllowingStateLoss();
    }

    private static FragmentManager getFragmentManager(FragmentActivity activity, AbstractDialogFragment.DialogListener listener) {
        if (listener instanceof Fragment) {
            return ((Fragment) listener).getChildFragmentManager();
        }
        return activity.getSupportFragmentManager();
    }

    public static void showSendLinkDialog(FragmentActivity activity, int dialogId, String phone, String message, DoubleActionDialogListener callbacks) {
        DialogFragment d = SendLinkThroughDialog.getInstance(dialogId, phone, activity, message, callbacks);
        showDialog(getFragmentManager(activity, callbacks), d, null);
    }

    public static void showFeatureAwardDialog(FragmentActivity activity, Features.Feature feature) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(FEATURE_FRAME);
        if (fragment != null) {
            activity.getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
        NotificationAlertManager.playTone(NotificationAlertManager.Tone.FEATURE_UNLOCK, NotificationAlertManager.getVelocity(activity, AudioManager.STREAM_NOTIFICATION));
        DialogFragment d = FeatureAwardDialogFragment.getInstance(feature);
        FragmentTransaction tr = activity.getSupportFragmentManager().beginTransaction();
        tr.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        tr.replace(R.id.feature_frame, d, FEATURE_FRAME);
        tr.commitAllowingStateLoss();
    }

    public static boolean isFeatureAwardDialogShown(FragmentActivity activity) {
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm != null) {
            Fragment fragment = fm.findFragmentByTag(FEATURE_FRAME);
            if (fragment instanceof FeatureAwardDialogFragment) {
                return ((FeatureAwardDialogFragment) fragment).isShown();
            }
        }
        return false;
    }

    public static void showNextFeatureDialog(FragmentActivity activity, boolean justUnlockedFeature) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(FEATURE_FRAME);
        if (fragment != null) {
            activity.getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
        FragmentTransaction tr = activity.getSupportFragmentManager().beginTransaction();
        tr.setCustomAnimations(R.anim.slide_up, R.anim.slide_down);
        tr.replace(R.id.feature_frame, NextFeatureDialogFragment.getInstance(justUnlockedFeature), FEATURE_FRAME);
        tr.commitAllowingStateLoss();
    }

    public static void showBlockingDialog(FragmentActivity activity, int titleId, int messageId) {
        if (activity.getSupportFragmentManager().findFragmentByTag("blocking") == null) {
            String title = activity.getString(titleId);
            String message = activity.getString(messageId);
            DialogFragment d = BlockingInfoDialog.getInstance(title, message);
            showDialog(activity.getSupportFragmentManager(), d, "blocking");
        }
    }
}
