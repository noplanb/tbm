package com.zazoapp.client.network.aws;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.network.HttpRequest;

public class S3CredentialsGetter {

	private final String TAG = getClass().getSimpleName();

	private Context context;
	
	public S3CredentialsGetter(Context c, boolean force){
		context = c;
		getCredentials(force);
	}
	
	// Should be overriden by subclasses if desired.
	protected void success(){
	    
	}
	
	protected void failure(){
	    
	}

    private void getCredentials(boolean force) {
        LinkedTreeMap<String, String> params;
        S3CredentialsStore videos = S3CredentialsStore.getInstance(context);
        final S3CredentialsStore avatars = S3AvatarDownloadHelper.getCredentialsStore(context);
        String uri;
        final Runnable getAvatars;
        if (force || !avatars.hasCredentials()) {
            getAvatars = new Runnable() {
                @Override
                public void run() {
                    LinkedTreeMap<String, String> params = new LinkedTreeMap<>();
                    String uri = new Uri.Builder().appendPath("s3_credentials").appendPath("avatars").build().toString();
                    new GetCredentials(uri, params, avatars, new RequestCallbacks() {
                        @Override
                        public void onSuccess() {
                            success();
                        }

                        @Override
                        public void onFailure() {
                            failure();
                        }
                    });
                }
            };
        } else {
            getAvatars = null;
        }
        if (force || !videos.hasCredentials()) {
            params = new LinkedTreeMap<>();
            uri = new Uri.Builder().appendPath("s3_credentials").appendPath("videos").build().toString();
            new GetCredentials(uri, params, videos, new RequestCallbacks() {
                @Override
                public void onSuccess() {
                    if (getAvatars == null) {
                        success();
                    } else {
                        getAvatars.run();
                    }
                }

                @Override
                public void onFailure() {
                    failure();
                }
            });
        } else {
            if (getAvatars != null) {
                getAvatars.run();
            }
        }
    }

    class GetCredentials extends HttpRequest {

        public GetCredentials(String uri, LinkedTreeMap<String, String> params, final S3CredentialsStore store, final RequestCallbacks callbacks) {
            super(uri, params, new Callbacks() {
                @Override
                public void success(String r) {
                    Gson g = new Gson();
                    Response response = null;
                    try {
                        response = g.fromJson(r, Response.class);
                    } catch (JsonSyntaxException e) {
                    }

                    Log.i(TAG, "gotCredentials");

                    if (response == null || !response.getStatus().equalsIgnoreCase("success")){
                        Dispatch.dispatch("CredentialsGetter: got failure from server in gotCredentials()");
                        callbacks.onFailure();
                        return;
                    }
                    response.saveCredentials(context, store);

                    if (!store.hasCredentials()){
                        Dispatch.dispatch("CredentialsGetter: !hasCredentials() when checking credentials in store after getting: " + response.toString());
                        callbacks.onFailure();
                        return;
                    }
                    callbacks.onSuccess();
                }
                @Override
                public void error(String errorString) {
                    callbacks.onFailure();
                }
            });
		}
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

        public void saveCredentials(Context context, S3CredentialsStore spm) {
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

    interface RequestCallbacks {
        void onSuccess();
        void onFailure();
    }
}
