package com.noplanbees.tbm.network.aws;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by User on 1/12/2015.
 */
public class SharedPreferenceManager {

    private static final String SECRET_KEY = "secret_key";
    private static final String ACCESS_KEY_ID = "access_key";
    private static final String BUCKET_NAME = "bucket_name";
    private static final String REGION = "region";

    private static SharedPreferenceManager sharedPreferenceManager;

    private final Context context;
    private SharedPreferences sp;

    private SharedPreferenceManager(Context context){
        this.context = context;
        sp = context.getSharedPreferences("zazo", Context.MODE_PRIVATE);
    }

    public static SharedPreferenceManager getSharedPreferenceManager(Context context){
        if(sharedPreferenceManager == null)
            sharedPreferenceManager = new SharedPreferenceManager(context);
        return sharedPreferenceManager;
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
}
