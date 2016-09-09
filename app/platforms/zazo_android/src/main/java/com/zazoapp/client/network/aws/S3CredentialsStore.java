package com.zazoapp.client.network.aws;

import android.content.Context;
import com.zazoapp.client.core.PreferencesHelper;

/**
 * Created by User on 1/12/2015.
 */
public class S3CredentialsStore {

    public static final String SECRET_KEY = "secret_key";
    public static final String ACCESS_KEY_ID = "access_key";
    public static final String BUCKET_NAME = "bucket_name";
    public static final String REGION = "region";

    private static S3CredentialsStore s3CredentialsStore;

    private PreferencesHelper preferences;
    private String prefix = "";

    private S3CredentialsStore(Context context){
        preferences = new PreferencesHelper(context);
    }

    public static S3CredentialsStore getInstance(Context context){
        if(s3CredentialsStore == null)
            s3CredentialsStore = new S3CredentialsStore(context);
        return s3CredentialsStore;
    }

    public static S3CredentialsStore newInstance(Context context, String prefix) {
        S3CredentialsStore store = new S3CredentialsStore(context);
        store.prefix = prefix + "_";
        return store;
    }

    public void saveS3AccessKey(String access_key){
        preferences.putString(prefix + ACCESS_KEY_ID, access_key);
    }

    public String getS3AccessKey(){
        return preferences.getString(prefix + ACCESS_KEY_ID, "");
    }

    public void saveS3SecretKey(String secret_key){
        preferences.putString(prefix + SECRET_KEY, secret_key);
    }

    public String getS3SecretKey(){
        return preferences.getString(prefix + SECRET_KEY, "");
    }

    public void saveS3Bucket(String bucket){
        preferences.putString(prefix + BUCKET_NAME, bucket);
    }

    public String getS3Bucket(){
        return preferences.getString(prefix + BUCKET_NAME, "");
    }

    public void saveS3Region(String region){
        preferences.putString(prefix + REGION, region);
    }

    public String getS3Region(){
        return preferences.getString(prefix + REGION, "");
    }
    
    public boolean hasCredentials(){
    	return !isBlank(getS3AccessKey()) && !isBlank(getS3Bucket()) && !isBlank(getS3Region()) && !isBlank(getS3SecretKey());
    }
    
    public boolean isBlank(String s){
    	return s == null || s.isEmpty();
    }
}
