package com.noplanbees.tbm.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

public class VersionDialogFragment extends DialogFragment {

    public interface Callbacks{
        void onPositiveButtonClicked();
    }

    public static final String IS_NEGATIVE_BUTTON = "is_negative_button";
    public static final String MESSAGE = "message";
    private boolean negativeButton;
    private String message;
    private Callbacks callbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        negativeButton = getArguments().getBoolean(IS_NEGATIVE_BUTTON, false);
        message = getArguments().getString(MESSAGE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callbacks = (Callbacks) activity;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Can't cast " + activity.getClass().getSimpleName()
                + " to VersionDialogFragment.Callbacks");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update Available")
                .setMessage(message)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        callbacks.onPositiveButtonClicked();
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
}
