package com.noplanbees.tbm.crash_dispatcher;

import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.Server;
import com.noplanbees.tbm.User;
import com.noplanbees.tbm.UserFactory;

/**
 * Created by User on 1/12/2015.
 */
public class Dispatch {
    private static final String TAG = Dispatch.class.getSimpleName();

    private static boolean isEnabled;

    public static void enable(){
        isEnabled = true;
    }

    public static void disable(){
        isEnabled = false;
    }

    public static void dispatch(String msg){
        if(isEnabled){
            LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
            params.put(User.Attributes.MKEY, UserFactory.current_user().get(User.Attributes.MKEY));
            params.put(User.Attributes.AUTH, UserFactory.current_user().get(User.Attributes.AUTH));
            params.put("msg", msg+"\n" + LogCatCollector.collectLogCat(null));
            String uri = new Uri.Builder().appendPath("dispatch").appendPath("post_dispatch").build().toString();
            new DispatchPost(uri, params);
        }
    }

//    Request:
//    POST url: “dispatch/post_dispatch” params: {auth:user_auth, mkey:user_mkey, msg: (the message the client wishes the server to email to us)}
//    Response:
//    {status: “success” | “failure”}

    private static class DispatchPost extends Server {

        public DispatchPost(String uri, LinkedTreeMap<String, String> params) {
            super(uri, params, "POST");
        }

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
            Log.e(TAG, "ERROR: DispatchPost: " + errorString);
        }
    }

    private static class Response{
        private String status;

        public String getStatus() {
            return status;
        }
    }

}
