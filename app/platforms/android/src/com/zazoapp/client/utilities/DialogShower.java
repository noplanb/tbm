package com.zazoapp.client.utilities;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Toast;
import com.zazoapp.client.R;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.multimedia.CameraException;
import com.zazoapp.client.ui.dialogs.AbstractDialogFragment;
import com.zazoapp.client.ui.dialogs.ActionInfoDialogFragment;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment.DoubleActionDialogListener;
import com.zazoapp.client.ui.dialogs.InfoDialogFragment;
import com.zazoapp.client.ui.dialogs.SelectPhoneNumberDialog;
import com.zazoapp.client.ui.dialogs.VersionDialogFragment;

/**
 * This utility class combines all popups which are showed to user
 * Created by skamenkovych@codeminders.com on 2/10/2015.
 */
public class DialogShower {
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
        DialogFragment dialog = ActionInfoDialogFragment.getInstance(title, message, action, actionId,
                needCancel, editable, listener);
        dialog.show(activity.getFragmentManager(), null);
    }

    public static void showDoubleActionDialog(Activity activity, String title, String message, String actionPositive,
                                              String actionNegative, int dialogId, boolean editable, DoubleActionDialogListener listener) {
        DialogFragment dialog = DoubleActionDialogFragment.getInstance(dialogId, title, message, actionPositive, actionNegative, editable, listener);
        dialog.show(activity.getFragmentManager(), null);
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
                DialogFragment dialogFragment = DoubleActionDialogFragment.getInstance(id, title, message, positiveText, negativeText, listener);
                dialogFragment.show(activity.getFragmentManager(), null);
            }
        });
    }

    public static void showInfoDialog(Activity activity, String title, String message) {
        DialogFragment info = InfoDialogFragment.getInstance(title, message);
        info.show(activity.getFragmentManager(), null);
    }

    public static void showHintDialog(Activity activity, String title, String message) {
        if (activity.getFragmentManager().findFragmentByTag("hint") == null) {
            DialogFragment info = InfoDialogFragment.getInstance(title, message);
            info.show(activity.getFragmentManager(), "hint");
        }
    }

    public static void showVersionHandlerDialog(Activity activity, String message, boolean negativeButton) {
        if (activity.getFragmentManager().findFragmentByTag("compatibility") == null) {
            DialogFragment d = VersionDialogFragment.getInstance(message, negativeButton);
            d.show(activity.getFragmentManager(), "compatibility");
        }
    }

    public static void showSelectPhoneNumberDialog(Activity activity, Contact contact, SelectPhoneNumberDialog.Callbacks callbacks) {
        DialogFragment f = SelectPhoneNumberDialog.getInstance(contact, callbacks);
        f.show(activity.getFragmentManager(), null);
    }
}
