package com.noplanbees.tbm.ui.dialogs;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class EnterCodeDialogFragment extends AbstractDialogFragment {

	public static final String PHONE_NUMBER = "phonenumber";
	
	public interface Callbacks{
		void didEnterCode(String code);
	} 
	
	private EditText verificationCodeTxt;
	private Callbacks callbacks;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		callbacks = (Callbacks) activity;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		String e164 = getArguments().getString(PHONE_NUMBER);
		
		setTitle("Enter code");
		
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		LinearLayout ll = new LinearLayout(getActivity());
		ll.setLayoutParams(lp);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(20, 20, 20, 20);

		TextView msgTxt = new TextView(getActivity());
		msgTxt.setText("We sent a code via text message to\n\n" + phoneWithFormat(e164, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL) + ".");
		msgTxt.setLayoutParams(lp);
		msgTxt.setPadding(15, 20, 15, 50);
		msgTxt.setTextSize(17);
		msgTxt.setGravity(Gravity.CENTER);

		verificationCodeTxt = new EditText(getActivity());
		verificationCodeTxt.setLayoutParams(lp);
		verificationCodeTxt.setHint("Enter code");
		msgTxt.setGravity(Gravity.CENTER_HORIZONTAL);
		verificationCodeTxt.setInputType(InputType.TYPE_CLASS_NUMBER);

		ll.addView(msgTxt);
		ll.addView(verificationCodeTxt);
		
		setCustomView(ll);
		
		setPositiveButton("Enter", new OnClickListener() {
			@Override
			public void onClick(View v) {
				callbacks.didEnterCode(verificationCodeTxt.getText().toString().replaceAll("\\s+", ""));
				dismiss();
			}
		});
		setNegativeButton("Cancel", null);
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
