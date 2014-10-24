package com.noplanbees.tbm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class SelectPhoneNumberDialog implements OnItemClickListener{
	private final String TAG = getClass().getSimpleName();

	public interface SelectPhoneNumberDelegate{
		public void phoneSelected(Contact contact, int phoneIndex);
	}

	public static final float PERCENT_OF_WINDOW_WIDTH = 0.75f;
	public static final float PERCENT_OF_WINDOW_HEIGHT = 0.4f;

	private Activity activity;
	private Contact contact;
	private SelectPhoneNumberDelegate delegate;
	private AlertDialog dialog;
	private ListView listView;
	private PhoneNumberListAdapter pnla;

	public SelectPhoneNumberDialog(Activity a, Contact c, SelectPhoneNumberDelegate delegate){
		activity = a;
		contact = c;
		this.delegate = delegate;
		setupDialogAndShow();
	}

	private void setupDialogAndShow(){
		AlertDialog.Builder db = new AlertDialog.Builder(activity);
		db.setTitle(contact.getFirstName() + "'s mobile?").
		setNegativeButton("Cancel", null);
		listView =  new ListView(activity);
		LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		listView.setLayoutParams(lp);
		listView.setOnItemClickListener(this);
		db.setView(listView);
		pnla = new PhoneNumberListAdapter(activity, contact);
		listView.setAdapter(pnla);
		dialog = db.show();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		hide();
		if (delegate != null)
			delegate.phoneSelected(contact, position);
	}

	public void hide(){
		if (dialog != null)
			dialog.dismiss();
	}


	//-----------------------
	// PhoneNumberListAdapter
	//-----------------------
	public class PhoneNumberListAdapter extends BaseAdapter{

		private Activity activity;
		private Contact contact;

		public PhoneNumberListAdapter(Activity activity, Contact contact){
			super();
			this.activity = activity;
			this.contact = contact;
		}

		@Override
		public int getCount() {
			return contact.phoneObjects.size();
		}

		@Override
		public Object getItem(int position) {
			return contact.phoneObjects.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null){
				LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.phone_list_item, parent, false);
			}

			TextView leftText = (TextView)convertView.findViewById(R.id.left_text);
			TextView rightText = (TextView)convertView.findViewById(R.id.right_text);
			leftText.setText(contact.phoneObjects.get(position).get(Contact.PhoneNumberKeys.NATIONAL));
			rightText.setText(contact.phoneObjects.get(position).get(Contact.PhoneNumberKeys.PHONE_TYPE));
			return convertView;
		}

	}

}
