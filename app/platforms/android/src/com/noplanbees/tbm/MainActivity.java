package com.noplanbees.tbm;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	public HomeController homeController;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		homeController = new HomeController(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		homeController.pause();
	}

};
