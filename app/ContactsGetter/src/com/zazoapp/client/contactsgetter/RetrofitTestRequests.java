package com.zazoapp.client.contactsgetter;

import android.os.AsyncTask;
import android.util.Log;
import com.amazonaws.http.HttpHeader;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.http.GET;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Created by skamenkovych@codeminders.com on 10/2/2015.
 */
public class RetrofitTestRequests {

    public static final String TAG = RetrofitTestRequests.class.getSimpleName();

    public interface IKvStoreRestAPI {
        @GET("/kvstore/received_videos")
        Call<ArrayList<LinkedTreeMap<String, Object>>> getReceivedVideos();
    }

    public static class GetAllRemoteIncomingVideoIds {

        public GetAllRemoteIncomingVideoIds() {
            new HttpRequest("kvstore/received_videos", new HttpRequest.Callbacks() {
                @Override
                public void success(String response) {
                    Gson g = new Gson();
                    ArrayList<LinkedTreeMap<String, Object>> kvs = null;
                    try {
                        kvs = g.fromJson(response, ArrayList.class);
                        if (kvs != null) {
                            gotRemoteKVs(kvs);
                        } else {
                            error("kvs null");
                        }
                    } catch (JsonSyntaxException e) {
                        error("JsonSyntaxException");
                    }
                }

                @Override
                public void error(String errorString) {
                    Log.e(TAG, "GetAllRemoteIncomingVideoIds: " + errorString);
                }
            });
        }

        private void gotRemoteKVs(ArrayList<LinkedTreeMap<String, Object>> kvs) {
            Log.d(TAG, "http: " + kvs);
        }
    }

    public static void testRun() {
        //new GetAllRemoteIncomingVideoIds();
        AsyncTaskManager.executeAsyncTask(false, new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("ZcAK4dM9S4m0IFui6ok6", "N4Yv70FPWlaxOS9M3wvS".toCharArray());
                    }
                });
                try {
                    Authenticator.requestPasswordAuthentication(Config.getServerHost(), InetAddress.getByName(Config.getServerHost()), -1, null, "zazo.com", "digest");
                    IKvStoreRestAPI kvStore = getKvStore();
                    kvStore.getReceivedVideos().enqueue(new Callback<ArrayList<LinkedTreeMap<String, Object>>>() {
                        @Override
                        public void onResponse(retrofit.Response<ArrayList<LinkedTreeMap<String, Object>>> response, Retrofit retrofit) {
                            Log.d(TAG, "rest: " + response.body());
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.e(TAG, "http: failure", throwable);
                        }
                    });
                } catch (UnknownHostException e) {
                    Log.e(TAG, "", e);
                }

                return null;
            }
        });
    }

    public static IKvStoreRestAPI getKvStore() {
        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request newRequest = chain.request().newBuilder()
                        .addHeader(HttpHeader.USER_AGENT, "Android")
                        .addHeader("Accept-Charset", "UTF-8").build();
                return chain.proceed(newRequest);
            }
        });
        client.setAuthenticator(new com.squareup.okhttp.Authenticator() {
            @Override
            public Request authenticate(Proxy proxy, Response response) throws IOException {
                return null;
            }

            @Override
            public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
                return null;
            }
        });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Config.getServerUri())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        IKvStoreRestAPI kvStore = retrofit.create(IKvStoreRestAPI.class);
        return kvStore;
    }
}


