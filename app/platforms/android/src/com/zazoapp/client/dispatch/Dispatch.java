package com.zazoapp.client.dispatch;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.network.HttpRequest;

import java.util.concurrent.CountDownLatch;

/**
 * Created by User on 1/12/2015.
 */
public class Dispatch {
    private static final String TAG = Dispatch.class.getSimpleName();

    private static boolean isEnabled = true;

    public static void enable(){
        isEnabled = true;
    }

    public static void disable(){
        isEnabled = false;
    }

    private static CountDownLatch countDownLatch;

    public static void dispatch(String msg){
        dispatch(msg, false);
    }

    public static void dispatch(String msg, boolean needToWait){
        Log.e(TAG, msg);
        if(isEnabled){
            LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
            params.put("msg", msg+"\n" + LogCatCollector.collectLogCat(null));
            String uri = new Uri.Builder().appendPath("dispatch").appendPath("post_dispatch").build().toString();
            countDownLatch = new CountDownLatch(1);
            new DispatchPost(uri, params);
            if(needToWait)
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }


    private static class DispatchPost extends HttpRequest {

        public DispatchPost(String uri, LinkedTreeMap<String, String> params) {
            super(uri, params, "POST", new Callbacks() {

                @Override
                public void success(String response_string) {
                    Log.i(TAG, "DispatchPost " + response_string);
                    Gson g = new Gson();
                    Response r = g.fromJson(response_string, Response.class);
                    if(r.getStatus().equals("success")){

                    }else{

                    }
                }

                @Override
                public void error(String errorString){
                }
            });
        }

        @Override
        protected void threadTaskDone() {
            super.threadTaskDone();
            countDownLatch.countDown();
        }

    }

    private static class Response{
        private String status;

        public String getStatus() {
            return status;
        }
    }

}
