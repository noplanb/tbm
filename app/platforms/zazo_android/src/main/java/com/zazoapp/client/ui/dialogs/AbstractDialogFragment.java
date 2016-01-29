package com.zazoapp.client.ui.dialogs;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.zazoapp.client.R;
import com.zazoapp.client.utilities.Convenience;

abstract public class AbstractDialogFragment extends DialogFragment implements TextWatcher {

    private static final String TARGET_TYPE = "target_type";
    private static final String ID = "dialog_id";
    private static final String EDITED_MESSAGE = "edited_message";
    private static final String EDITABLE = "editable";

    @InjectView(R.id.btn_dialog_cancel) Button btnCancel;
    @InjectView(R.id.btn_dialog_ok) Button btnOk;
    @InjectView(R.id.dlg_title) TextView twTitle;
    @InjectView(R.id.dlg_msg) TextView twMsg;
    @InjectView(R.id.dlg_edit_msg_floating_label) TextInputLayout textInputLayout;
    @InjectView(R.id.dlg_edit_msg) EditText editMsg;
    @InjectView(R.id.dlg_body) LinearLayout body;
    @InjectView(R.id.dlg_buttons_layout) ViewGroup buttonsLayout;

    private DialogListener listener;

    public interface DialogListener {
        // Just a marker
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.base_dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_body, container, true);
        ButterKnife.inject(this, v);
        editMsg.addTextChangedListener(this);
        btnOk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        Typeface tf = Convenience.getTypeface(v.getContext());
        btnOk.setTypeface(tf);
        btnCancel.setTypeface(tf);
        tf = Convenience.getTypeface(v.getContext(), "Roboto-Regular");
        editMsg.setTypeface(tf);
        twTitle.setTypeface(tf, Typeface.BOLD);
        twMsg.setTypeface(tf);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (getArguments() != null) {
            DialogListenerType type = (DialogListenerType) getArguments().getSerializable(TARGET_TYPE);
            if (type != null) {
                switch (type) {
                    case ACTIVITY:
                        listener = (DialogListener) activity;
                        break;
                    case FRAGMENT:
                        listener = (DialogListener) getParentFragment();
                        break;
                }
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    protected DialogListener getListener() {
        return listener;
    }

    protected void setTitle(String title){
        if(title!=null) {
            twTitle.setVisibility(View.VISIBLE);
            twTitle.setText(title);
        }
	}

    protected void setMessage(String message) {
        boolean editable = getArguments() != null && getArguments().getBoolean(EDITABLE);
        if (message != null) {
            if (editable) {
                editMsg.setVisibility(View.VISIBLE);
                editMsg.setText(message);
            } else {
                twMsg.setVisibility(View.VISIBLE);
                twMsg.setText(message);
            }

        }
    }

    protected void setPositiveButton(String name, OnClickListener clickListener){
		if(name != null){
			btnOk.setVisibility(View.VISIBLE);
			btnOk.setText(name);
		}else
            btnOk.setVisibility(View.GONE);

        if(clickListener != null)
			btnOk.setOnClickListener(clickListener);
	}
	
	protected void setNegativeButton(String name, OnClickListener clickListener){
		if(name != null){
			btnCancel.setVisibility(View.VISIBLE);
			btnCancel.setText(name);
		}
		if(clickListener != null)
			btnCancel.setOnClickListener(clickListener);
	}
	
	protected void setCustomView(View ll) {
		body.addView(ll, new LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
	}

    /**
     * Set up dialog listener. Call this only after setting arguments
     * @param listener
     * @param dialogId
     */
    protected void setDialogListener(DialogListener listener, int dialogId) {
        DialogListenerType type = DialogListenerType.getType(listener);
        if (type == DialogListenerType.NONE && listener != null) {
            throw new ClassCastException("DialogListener must be overridden by Fragment or Activity");
        }
        getArguments().putSerializable(TARGET_TYPE, type);
        getArguments().putInt(ID, dialogId);
    }

    protected int getDialogId() {
        if (getArguments() != null)
            return getArguments().getInt(ID, -1);
        return -1;
    }

    protected void setEditable(boolean editable) {
        if (getArguments() != null)
            getArguments().putBoolean(EDITABLE, editable);
    }

    protected boolean isEditable() {
        return getArguments() != null && getArguments().getBoolean(EDITABLE);
    }

    protected String getEditedMessage() {
        return editMsg.getText().toString();
    }

    public static String getEditedMessage(Bundle bundle) {
        if (bundle != null) {
            return bundle.getString(EDITED_MESSAGE, "");
        }
        return "";
    }

    protected static void putEditedMessage(Bundle bundle, String message) {
        bundle.putString(EDITED_MESSAGE, message);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        int textLength = s.toString().length();
        btnOk.setEnabled(!isEditable() || textLength != 0 && positiveButtonMightBeEnabled());
    }

    protected boolean positiveButtonMightBeEnabled() {
        return true;
    }
}
