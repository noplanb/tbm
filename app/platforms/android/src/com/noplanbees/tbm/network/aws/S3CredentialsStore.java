package com.noplanbees.tbm.network.aws;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by User on 1/12/2015.
 */
public class S3CredentialsStore {

    private static final String SECRET_KEY = "secret_key";
    private static final String ACCESS_KEY_ID = "access_key";
    private static final String BUCKET_NAME = "bucket_name";
    private static final String REGION = "region";

    private static S3CredentialsStore s3CredentialsStore;

    private SharedPreferences sp;

    private S3CredentialsStore(Context context){
        sp = context.getSharedPreferences("zazo", Context.MODE_PRIVATE);
    }

    public static S3CredentialsStore getInstance(Context context){
        if(s3CredentialsStore == null)
            s3CredentialsStore = new S3CredentialsStore(context);
        return s3CredentialsStore;
    }

    public void saveS3AccessKey(String access_key){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ACCESS_KEY_ID, access_key);
        editor.commit();
    }

    public String getS3AccessKey(){
        return sp.getString(ACCESS_KEY_ID, "");
    }

    public void saveS3SecretKey(String secret_key){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SECRET_KEY, secret_key);
        editor.commit();
    }

    public String getS3SecretKey(){
        return sp.getString(SECRET_KEY, "");
    }

    public void saveS3Bucket(String bucket){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(BUCKET_NAME, bucket);
        editor.commit();
    }

    public String getS3Bucket(){
        return sp.getString(BUCKET_NAME, "");
    }

    public void saveS3Region(String region){
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(REGION, region);
        editor.commit();
    }

    public String getS3Region(){
        return sp.getString(REGION, "");
    }
    
    public boolean hasCredentials(){
    	return getS3AccessKey() != null && getS3Bucket() != null && getS3Region() != null && getS3SecretKey() != null;
    }
}
