package com.noplanbees.tbm.utilities;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Toast;
import com.noplanbees.tbm.R;
import com.noplanbees.tbm.multimedia.CameraException;
import com.noplanbees.tbm.ui.dialogs.ActionInfoDialogFragment;
import com.noplanbees.tbm.ui.dialogs.DoubleActionDialogFragment;
import com.noplanbees.tbm.ui.dialogs.DoubleActionDialogFragment.DoubleActionDialogListener;
import com.noplanbees.tbm.ui.dialogs.InfoDialogFragment;
import com.noplanbees.tbm.ui.dialogs.VersionDialogFragment;

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

    public static void showBadConnection(Activity activity) {
        Resources res = activity.getResources();
        showActionInfoDialog(activity, res.getString(R.string.dialog_bad_connection_title),
                res.getString(R.string.dialog_bad_connection_message), res.getString(R.string.dialog_action_try_again),
                false, -1);
    }

    public static void showActionInfoDialog(Activity activity, String title, String message, String action, boolean needCancel, int actionId) {
        ActionInfoDialogFragment actionDialogFragment = new ActionInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ActionInfoDialogFragment.TITLE, title);
        args.putString(ActionInfoDialogFragment.MSG, message);
        args.putString(ActionInfoDialogFragment.ACTION, action);
        args.putBoolean(ActionInfoDialogFragment.NEED_CANCEL, needCancel);
        if (actionId != -1)
            args.putInt(ActionInfoDialogFragment.ID, actionId);
        actionDialogFragment.setArguments(args);
        actionDialogFragment.show(activity.getFragmentManager(), null);
    }

    public static void showCameraException(final Activity activity, CameraException cameraException, final DoubleActionDialogListener listener) {
        Resources res = activity.getResources();
        final String title = res.getString(cameraException.getTitleId());
        final String message = res.getString(cameraException.getMessageId());
        final String positiveText = res.getString(R.string.dialog_action_try_again);
        final String negativeText = res.getString(R.string.dialog_action_quit);

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                DoubleActionDialogFragment dialogFragment = new DoubleActionDialogFragment();
                dialogFragment.setListener(listener);
                Bundle args = new Bundle();
                args.putString(DoubleActionDialogFragment.TITLE, title);
                args.putString(DoubleActionDialogFragment.MSG, message);
                args.putString(DoubleActionDialogFragment.ACTION_POSITIVE, positiveText);
                args.putString(DoubleActionDialogFragment.ACTION_NEGATIVE, negativeText);
                dialogFragment.setArguments(args);
                dialogFragment.show(activity.getFragmentManager(), null);
            }
        });
    }

    public static void showInfoDialog(Activity activity, String title, String message) {
        InfoDialogFragment info = new InfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(InfoDialogFragment.TITLE, title);
        args.putString(InfoDialogFragment.MSG, message);
        info.setArguments(args );
        info.show(activity.getFragmentManager(), null);
    }

    public static void showVersionHandlerDialog(Activity activity, String message, boolean negativeButton) {
        DialogFragment d = new VersionDialogFragment();
        Bundle b = new Bundle();
        b.putBoolean(VersionDialogFragment.IS_NEGATIVE_BUTTON, negativeButton);
        b.putString(VersionDialogFragment.MESSAGE, message);
        d.setArguments(b);
        d.show(activity.getFragmentManager(),null);
    }
}
