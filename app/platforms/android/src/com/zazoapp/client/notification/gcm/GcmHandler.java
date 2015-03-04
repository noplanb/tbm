package com.zazoapp.client.notification.gcm;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.utilities.AsyncTaskManager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class GcmHandler {

	//	GCM attributes
	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	
	private Activity activity;
	private final String TAG = "GCM";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
//	private String SENDER_ID = "550639704405"; // ThreeByMe on console.developers.google.com
	private String SENDER_ID = "462139407481"; // Zazo on console.developers.google.com
	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	private SharedPreferences prefs;
	private String regid;

	public GcmHandler(Activity a){
		activity = a;
	}


	public boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				Log.i(TAG, "This device is not supported.");
			}
			return false;
		}
		return true;
	}

	public String registerGcm(){
		Log.i(TAG, "registerGCM");
		gcm = GoogleCloudMessaging.getInstance(activity);
		regid = getRegistrationId(activity);
		Log.i(TAG, "registerGCM: got GCM registration id = " + regid);
		
		if (regid.isEmpty()) {
			registerInBackground();
		} else {
			sendRegistrationIdToBackend();
		}
		return regid;
	}

	/**
	 * Gets the current registration ID for application on GCM service.
	 * <p>
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getGCMPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
		// This sample app persists the registration ID in shared preferences, but
		// how you store the regID in your app is up to you.
		return activity.getSharedPreferences(activity.getClass().getSimpleName(),
				Context.MODE_PRIVATE);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
		AsyncTaskManager.executeAsyncTask(new RIBAsync(), new Void[]{});
	}


	private class RIBAsync extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (gcm == null) {
					gcm = GoogleCloudMessaging.getInstance(activity);
				}
				regid = gcm.register(SENDER_ID);
				Log.i(TAG, "Device registered, registration ID=" + regid);

				// Need to send registration id to our server over HTTP.
				sendRegistrationIdToBackend();

				// For this demo: we don't need to send it because the device
				// will send upstream messages to a server that echo back the
				// message using the 'from' address in the message.

				// Persist the regID - no need to register again.
				storeRegistrationId(activity, regid);
			} catch (IOException ex) {
				// If there is an error wait for the user to turn off the app to try again.
				Dispatch.dispatch("Error :" + ex.getMessage());
			}
			return null;
		}
	}
	
	private void sendRegistrationIdToBackend() {
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put("mkey", UserFactory.current_user().get(User.Attributes.MKEY));
		params.put("push_token", regid);
		params.put("device_platform", "android");
	    new GCMPostPushToken("notification/set_push_token", params, "POST");
	}
	
	class GCMPostPushToken extends HttpRequest {
		public GCMPostPushToken(String uri, LinkedTreeMap<String, String> params, String method) {
			super(uri, params, method, new Callbacks() {
                @Override
                public void success(String response) {
                    Log.i(TAG, "postPushToken: got response = " + response);
                }
                @Override
                public void error(String errorString) {
                    Dispatch.dispatch("ERROR: postPushToken: " + errorString);
                }
            });
		}
	}
	
	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
	}

	/**
	 * Send an echo message from the device to the server. We dont use this. However it is good for testing purposes.
	 * Sending to a loopback server echos the message back doesn't require the server to have the device's
	 * push_token.
	 */
	private class sendAsync extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
            try {
                Bundle data = new Bundle();
                    data.putString("my_message", "Hello World");
                    data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
                    String id = Integer.toString(msgId.incrementAndGet());
                    gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                    Log.i(TAG, "Sent message");
            } catch (IOException e) {
                Dispatch.dispatch(e.getMessage());
            }
			return null;
		}
	}
	
}
