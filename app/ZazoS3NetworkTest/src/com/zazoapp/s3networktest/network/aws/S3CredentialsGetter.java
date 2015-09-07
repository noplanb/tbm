package com.zazoapp.s3networktest.network.aws;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.s3networktest.dispatch.Dispatch;

public class S3CredentialsGetter {

	private final String TAG = getClass().getSimpleName();

	private Context context;
	
	public S3CredentialsGetter(Context c){
		context = c;
		getCredentials();
	}
	
	// Should be overriden by subclasses if desired.
	protected void success(){
	    
	}
	
	protected void failure(){
	    
	}

	private void getCredentials(){
		LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
		String uri = new Uri.Builder().appendPath("s3_credentials").appendPath("info").build().toString();
        gotCredentials(context, "{\"status\":\"success\",\"region\":\"us-west-1\",\"bucket\":\"staging-videos.zazo.com\",\"access_key\":\"AKIAII2H736MBQ7JHWSA\",\"secret_key\":\"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\"}");
		//new GetCredentials(uri, params);
	}
    //
	//class GetCredentials extends HttpRequest{
	//	public GetCredentials(String uri, LinkedTreeMap<String, String> params){
	//		super(uri, params, new Callbacks() {
     //           @Override
     //           public void success(String response) {
     //               gotCredentials(context, response);
     //           }
     //           @Override
     //           public void error(String errorString) {
     //               failure();
     //           }
     //       });
	//	}
	//}

	public void gotCredentials(Context context, String r) {
		Gson g = new Gson();
        Response response = null;
        try {
            response = g.fromJson(r, Response.class);
        } catch (JsonSyntaxException e) {
        }

        Log.i(TAG, "gotCredentials");

        if(response == null || !response.getStatus().equalsIgnoreCase("success")){
            Dispatch.dispatch("CredentialsGetter: got failure from server in gotCredentials()");
            failure();
            return;
        }

        response.saveCredentials(context);
        
        if (!S3CredentialsStore.getInstance(context).hasCredentials()){
            Dispatch.dispatch("CredentialsGetter: !hasCredentials() when checking credentials in store after getting: " + response.toString());
            failure();
            return;
        }
            
        success();
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
            S3CredentialsStore spm = S3CredentialsStore.getInstance(context);
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