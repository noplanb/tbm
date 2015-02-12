package com.noplanbees.tbm.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.noplanbees.tbm.VersionHandler;

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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update Available")
                .setMessage(message)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        goToPlayStore();
                    }
                });

        if (negativeButton){
            builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dismissAllowingStateLoss();
                }
            });
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        return alertDialog;
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
