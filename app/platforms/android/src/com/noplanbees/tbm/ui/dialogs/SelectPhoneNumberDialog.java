package com.noplanbees.tbm.ui.dialogs;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.noplanbees.tbm.R;
import com.noplanbees.tbm.model.Contact;

public class SelectPhoneNumberDialog extends AbstractDialogFragment implements AdapterView.OnItemClickListener {
    private final static String TAG = SelectPhoneNumberDialog.class.getSimpleName();

    private static final String CONTACT_KEY = "contact_key";

    public static DialogFragment getInstance(Contact contact){
        DialogFragment f = new SelectPhoneNumberDialog();
        Bundle args = new Bundle();
        args.putParcelable(CONTACT_KEY, contact);
        f.setArguments(args);
        return f;
    }

    public interface Callbacks {
		public void phoneSelected(Contact contact, int phoneIndex);
	}

	private Contact contact;
	private Callbacks callbacks;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (Callbacks) activity;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contact = getArguments().getParcelable(CONTACT_KEY);

        setTitle(contact.getFirstName() + "'s mobile?");
        setNegativeButton("Cancel", null);
        setPositiveButton(null, null);
        ListView listView = new ListView(getActivity());
		listView.setOnItemClickListener(this);
        setCustomView(listView);
        ListAdapter adapter = new PhoneNumberListAdapter(getActivity(), contact);
		listView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (callbacks != null)
            callbacks.phoneSelected(contact, position);
    }

    public class PhoneNumberListAdapter extends BaseAdapter{

		private Context activity;
		private Contact contact;

		public PhoneNumberListAdapter(Context context, Contact contact){
			this.activity = context;
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
