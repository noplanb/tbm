package com.zazoapp.client.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.zazoapp.client.R;

public class EnterCodeDialogFragment extends AbstractDialogFragment implements OnClickListener {

    private static final String PHONE_NUMBER = "phonenumber";

    public interface Callbacks extends DialogListener {
        void didEnterCode(String code);

        void requestCall();
    }

    public static DialogFragment getInstance(String phoneNumber, Callbacks callbacks) {
        AbstractDialogFragment fragment = new EnterCodeDialogFragment();
        Bundle args = new Bundle();
        args.putString(PHONE_NUMBER, phoneNumber);
        fragment.setArguments(args);
        fragment.setDialogListener(callbacks, 0);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String e164 = getArguments().getString(PHONE_NUMBER);

        setTitle(getString(R.string.enter_code_dlg_helper));
        setMessage(getString(R.string.enter_code_dlg_msg,
                phoneWithFormat(e164, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)));
        View v = LayoutInflater.from(getActivity()).inflate(R.layout.enter_code_dialog,
                null, false);
        final EditText edtVerificationCode = (EditText) v.findViewById(R.id.edt_code);

        final TextView callButton = ButterKnife.findById(v, R.id.call_btn);
        callButton.setOnClickListener(this);

        setCustomView(v);
        btnOk.setEnabled(false);
        edtVerificationCode.addTextChangedListener(enterCodeFieldWatcher);
        setPositiveButton(getString(R.string.dialog_action_enter), new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getListener() instanceof Callbacks) {
                    ((Callbacks) getListener()).didEnterCode(edtVerificationCode.getText().toString().replaceAll("\\s+", ""));
                }
                edtVerificationCode.setText("");
                edtVerificationCode.removeTextChangedListener(enterCodeFieldWatcher);
                dismiss();
            }
        });
        setNegativeButton(getString(R.string.dialog_action_cancel), new OnClickListener() {
            @Override
            public void onClick(View v) {
                edtVerificationCode.setText("");
                edtVerificationCode.removeTextChangedListener(enterCodeFieldWatcher);
                dismiss();
            }
        });
    }

    //-------------
    // Convenience
    //-------------
    private String phoneWithFormat(String phone, PhoneNumberFormat format) {
        if (phone == null)
            return null;

        PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
        try {
            PhoneNumber pn = pu.parse(phone, "US");
            return pu.format(pn, format);
        } catch (NumberParseException e) {
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.call_btn:
                if (getListener() instanceof Callbacks) {
                    v.setEnabled(false);
                    ((TextView) v).setText("");
                    ((Callbacks) getListener()).requestCall();
                }
                break;
        }
    }

    public void setCalling() {
        View view = getView();
        if (view != null) {
            final TextView callBtn = ButterKnife.findById(view, R.id.call_btn);
            callBtn.setText(getString(R.string.enter_code_dlg_button_calling));
        }
    }

    private TextWatcher enterCodeFieldWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            int textLength = s.toString().length();
            btnOk.setEnabled(textLength >= 4);
        }
    };
}
