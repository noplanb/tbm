package com.zazoapp.s3networktest.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkConfig {

    public static final boolean IS_AWS_USING = true;

    public static String getConnectionStatus(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo == null) ? "No connection" : networkInfo.toString();
    }
}
