package com.zazoapp.client.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.zazoapp.client.core.Settings;

public class NetworkConfig {

    public static final boolean IS_AWS_USING = true;

    public static String getConnectionStatus(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo == null) ? "No connection" : networkInfo.toString();
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected() && (!networkInfo.isRoaming() || Settings.Bool.ALLOW_DATA_IN_ROAMING.isSet());
    }
}
