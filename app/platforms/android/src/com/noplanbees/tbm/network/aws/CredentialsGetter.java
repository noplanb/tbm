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
	//ArrayList<LinkedTreeMap<String, String>> friendList = new ArrayList<LinkedTreeMap<String, String>>();

	public interface CredentialsGetterCallback{
		public void gotCredentials();
	}

	public CredentialsGetter(Context c, boolean isRetryable,  CredentialsGetterCallback delegate){
		context = c;
        this.isRetryable = isRetryable;
		this.delegate = delegate;
		progress = new ProgressDialog(context);
		progress.setTitle("Checking");
		getFriends();
	}

	private void getFriends(){
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		//params.put(UserFactory.ServerParamKeys.AUTH, UserFactory.current_user().get(User.Attributes.AUTH));
		//params.put(UserFactory.ServerParamKeys.MKEY, UserFactory.current_user().get(User.Attributes.MKEY));
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

    //    Response
//    {status: “success” | “failure”,
//        region: region,
//                bucket: bucket,
//            access_key: access_key,
//            secret_key: secret_key}
//    region values: us_east_1 us_west_1 us_west_2 ap_southeast_1 ap_southeast_2 ap_northeast_1 sa_east_1

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
