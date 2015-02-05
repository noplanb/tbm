package com.noplanbees.tbm.network.aws;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.network.Server;

public class CredentialsGetter {

	private final String TAG = getClass().getSimpleName();

	private Context context;
	private CredentialsGetterCallback delegate;

	public interface CredentialsGetterCallback{
		void success();
        void failure();
	}

	public CredentialsGetter(Context c, CredentialsGetterCallback delegate){
		context = c;
		this.delegate = delegate;
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
		}
		@Override
		public void success(String response) {
			gotCredentials(context, response);
		}
		@Override
		public void error(String errorString) {
            if(delegate!=null)
                delegate.failure();
		}
	}

	@SuppressWarnings("unchecked")
	public void gotCredentials(Context context, String r) {
		Gson g = new Gson();
		Response response = g.fromJson(r, Response.class);
		Log.i(TAG, "gotRegResponse: " + response.toString());

        if(response.getStatus().equals("“failure”")){
                if(delegate!=null)
                    delegate.failure();
            return;
        }

        response.saveCredentials(context);

        if(delegate!=null)
            delegate.success();
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
