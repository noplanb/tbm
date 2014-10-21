package com.noplanbees.tbm;

import android.app.Activity;
import android.graphics.Point;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.google.gson.internal.LinkedTreeMap;

public class SelectPhoneNumberDialog {

	public static final float PERCENT_OF_WINDOW = 0.75f;

	private Activity activity;
	private Contact contact;
	private RelativeLayout frame;
	private ListView listView;
	private TextView titleText;
	private String[] phoneNumbers;

	public SelectPhoneNumberDialog(Activity a, Contact contact){
		activity = a;
		this.contact = contact;
		frame = (RelativeLayout) activity.findViewById(R.id.select_phone_number_frame);
		titleText = (TextView) frame.findViewById(R.id.select_phone_number_title);
		listView = (ListView) frame.findViewById(R.id.select_phone_number_list_view);

		setupFrame();
		setupTitle();
		setupList();
	}


	//------------
	// Setup views
	//------------
	private void setupFrame(){
		LayoutParams lp = new LayoutParams(dialogWidth(), dialogHeight());
		frame.setLayoutParams(lp);
		frame.setX(xPos());
		frame.setY(yPos());
	}

	private void setupTitle(){
		titleText.setText(contact.getDisplayName());
	}

	private void setupList(){
		phoneNumbers = new String[contact.phoneObjects.size()];
		int i = 0;
		for(LinkedTreeMap<String, String> po : contact.phoneObjects){
			i++;	
		}
	}

	//----------
	// Show hide
	//----------
	private void show(){
		frame.setVisibility(View.VISIBLE);
	}

	private void hide(){
		frame.setVisibility(View.INVISIBLE);
	}

	private int dialogWidth(){
		return (int) (PERCENT_OF_WINDOW * windowSize().x);
	}

	private int dialogHeight(){
		return (int) (PERCENT_OF_WINDOW * windowSize().y);
	}

	private int xPos(){
		return (windowSize().x - dialogWidth())/2;
	}

	private int yPos(){
		return (windowSize().y - dialogWidth())/2;
	}

	private Point windowSize(){
		Point p = new Point();
		activity.getWindowManager().getDefaultDisplay().getSize(p);
		return p;
	}
}
