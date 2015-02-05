package com.noplanbees.tbm.network.aws;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.Config;
import com.noplanbees.tbm.network.Server;

public class CredentialsGetter {

	private final String TAG = getClass().getSimpleName();
    private final boolean isRetryable;

    private ProgressDialog progress;
	private Context context;
	private CredentialsGetterCallback delegate;

	public interface CredentialsGetterCallback{
		public void gotCredentials();
	}

	public CredentialsGetter(Context c, boolean isRetryable,  CredentialsGetterCallback delegate){
		context = c;
        this.isRetryable = isRetryable;
		this.delegate = delegate;
		progress = new ProgressDialog(context);
		progress.setTitle("Checking");
		getCredentials();
	}

	private void getCredentials(){
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		String uri = new Uri.Builder().appendPath("s3_infos").appendPath("info").build().toString();
		new GetCredentials(uri, params);
	}

	class GetCredentials extends Server{
		public GetCredentials(String uri, LinkedTreeMap<String, String> params){
			super(uri, params);
			//progress.show();
		}
		@Override
		public void success(String response) {
			//progress.dismiss();
			gotCredentials(context, response);
		}
		@Override
		public void error(String errorString) {
			//progress.dismiss();
			serverError();
		}
	}

	@SuppressWarnings("unchecked")
	public void gotCredentials(Context context, String r) {
		Gson g = new Gson();
		Response response = g.fromJson(r, Response.class);
		Log.i(TAG, "gotRegResponse: " + response.toString());

        if(response.getStatus().equals("“failure”")){
            if(isRetryable) {
                serverError();
            }else{
               //TODO:do not retry
            }

            return;
        }

        response.saveCredentials(context);

        if(delegate!=null)
            delegate.gotCredentials();
	}

	private void serverError(){
		showErrorDialog("No Connection", "Can't reach " + Config.appName + ".\n\nCheck your connection and try again.");
	}

	private void showErrorDialog(String title, String message){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title)
		.setMessage(message)
		.setPositiveButton("Ok", null)
		.create().show();
	}


    private class Response{
        private String status;
        private String region;
        private String bucket;
        private String access_key;
        private String secret_key;

        public String getStatus() {
            return status;
        }

        public String getRegion() {
            return region;
        }

        public String getBucket() {
            return bucket;
        }

        public String getAccess_key() {
            return access_key;
        }

        public String getSecret_key() {
            return secret_key;
        }

        public void saveCredentials(Context context) {
            SharedPreferenceManager spm = SharedPreferenceManager.getSharedPreferenceManager(context);
            spm.saveS3AccessKey(access_key);
            spm.saveS3SecretKey(secret_key);
            spm.saveS3Bucket(bucket);
            spm.saveS3Region(region);
        }

        @Override
        public String toString() {
            return status+", "+region + "," + bucket + ", " + access_key + ", " + secret_key;
        }
    }

}
