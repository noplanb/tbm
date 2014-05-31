package com.noplanbees.tbm;

import android.app.Activity;
import android.os.Bundle;
import android.widget.VideoView;

public class TouchTestActivity extends Activity {
    
	VideoView tt;
	LongpressTouchHandler lph;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.touch_test);

		lph = new LongpressTouchHandler(this, findViewById(R.id.tt));
	}


	
}
