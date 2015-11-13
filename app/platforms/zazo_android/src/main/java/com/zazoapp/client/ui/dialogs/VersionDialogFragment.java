package com.zazoapp.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import com.zazoapp.client.R;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.VersionHandler;

public class VersionDialogFragment extends DialogFragment {

    private static final String IS_NEGATIVE_BUTTON = "is_negative_button";
    private static final String MESSAGE = "message";

    public static DialogFragment getInstance(String message, boolean isNegativeButton){
        DialogFragment dialog = new VersionDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(VersionDialogFragment.IS_NEGATIVE_BUTTON, isNegativeButton);
        args.putString(VersionDialogFragment.MESSAGE, message);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean negativeButton = getArguments().getBoolean(IS_NEGATIVE_BUTTON, false);
        String message = getArguments().getString(MESSAGE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(getString(R.string.dialog_update_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_action_update), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        goToPlayStore();
                    }
                });

        if (negativeButton){
            builder.setNegativeButton(getString(R.string.dialog_action_later), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dismissAllowingStateLoss();
                    new PreferencesHelper(getActivity()).putBoolean(VersionHandler.UPDATE_SESSION, false);
                }
            });
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        return alertDialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        boolean hasNegativeButton = getArguments().getBoolean(IS_NEGATIVE_BUTTON, false);
        if (hasNegativeButton) {
            new PreferencesHelper(getActivity()).putBoolean(VersionHandler.UPDATE_SESSION, false);
        } else {
            getActivity().finish();
        }
    }

    private void goToPlayStore(){
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getActivity().getPackageName())));
        } catch (ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="
                    + getActivity().getPackageName())));
        }
        getActivity().finish();
    }
}
