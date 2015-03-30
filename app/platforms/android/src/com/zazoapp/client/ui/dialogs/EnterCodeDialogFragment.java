package com.zazoapp.client.ui.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.zazoapp.client.R;

public class EnterCodeDialogFragment extends AbstractDialogFragment {

	private static final String PHONE_NUMBER = "phonenumber";

	public interface Callbacks extends DialogListener {
		void didEnterCode(String code);
	}

    public static DialogFragment getInstance(String phoneNumber, Callbacks callbacks){
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

		setTitle("Enter Code");

		View v = LayoutInflater.from(getActivity()).inflate(R.layout.enter_code_dialog,
				null, false);
		TextView twMsg = (TextView) v.findViewById(R.id.tw_msg);
		final EditText edtVerificationCode = (EditText) v.findViewById(R.id.edt_code);

		twMsg.setText(getString(R.string.enter_code_dlg_msg,
				phoneWithFormat(e164, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)));

		setCustomView(v);

        setPositiveButton("Enter", new OnClickListener() {
			@Override
			public void onClick(View v) {
                if (getListener() instanceof Callbacks) {
                    ((Callbacks) getListener()).didEnterCode(edtVerificationCode.getText().toString().replaceAll("\\s+", ""));
                }
                edtVerificationCode.setText("");
                dismiss();
            }
        });
        setNegativeButton("Cancel", new OnClickListener() {
            @Override
            public void onClick(View v) {
                edtVerificationCode.setText("");
                dismiss();
            }
        });
    }

	//-------------
	// Convenience
	//-------------
	private String phoneWithFormat(String phone, PhoneNumberFormat format){
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
}
