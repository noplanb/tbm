package com.zazoapp.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.zazoapp.client.R;
import com.zazoapp.client.debug.DebugConfig;

/**
 * Created by skamenkovych@codeminders.com on 11/2/2015.
 */
public class BlockingInfoDialog extends DialogFragment {

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";

    public static DialogFragment getInstance(String title, String message) {
        DialogFragment dialog = new BlockingInfoDialog();
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(MESSAGE, message);
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
        String title = getArguments().getString(TITLE);
        String message = getArguments().getString(MESSAGE);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.dialog_action_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        getActivity().finish();
                    }
                });
        if (DebugConfig.getInstance(getActivity()).isDebugEnabled()) {
            builder.setNegativeButton(getString(R.string.dialog_action_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);

        return alertDialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getActivity().finish();
    }

}
