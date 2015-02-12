package com.noplanbees.tbm.ui.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.noplanbees.tbm.R;

abstract public class AbstractDialogFragment extends DialogFragment {

	private Button btnCancel;
	private Button btnOk;
	private TextView twTitle;
	private TextView twMsg;
	private FrameLayout body;
	private View btnsDivider;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.base_dialog);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.dialog_body, container, true);
		
		btnsDivider = v.findViewById(R.id.divider);
		btnCancel = (Button)v.findViewById(R.id.btn_dialog_cancel);
		btnOk = (Button)v.findViewById(R.id.btn_dialog_ok);
		
		twTitle = (TextView)v.findViewById(R.id.dlg_title);
		twMsg = (TextView)v.findViewById(R.id.dlg_msg);
		
		body = (FrameLayout)v.findViewById(R.id.dlg_body);
		
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

		return v;
	}	
	
	protected void setTitle(String title){
        if(title!=null) {
            twTitle.setVisibility(View.VISIBLE);
            twTitle.setText(title);
        }
	}
	
	protected void setMessage(String message){
        if(message!=null){
            twMsg.setVisibility(View.VISIBLE);
            twMsg.setText(message);
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
			btnsDivider.setVisibility(View.VISIBLE);
			btnCancel.setVisibility(View.VISIBLE);
			btnCancel.setText(name);
		}
		if(clickListener != null)
			btnCancel.setOnClickListener(clickListener);
	}
	
	protected void setCustomView(View ll) {
		body.addView(ll, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
	}
}
