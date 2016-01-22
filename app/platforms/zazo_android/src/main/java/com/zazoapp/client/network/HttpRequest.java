package com.zazoapp.client.network;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.utilities.AsyncTaskManager;
import com.zazoapp.client.utilities.Logger;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.auth.AuthScope;
import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.params.CoreProtocolPNames;
import cz.msebera.android.httpclient.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class HttpRequest {

    public interface Callbacks{
        void success(String response);
        void error(String errorString);
    }
    
    private String method;

    private static final String TAG = HttpRequest.class.getSimpleName();
    private String pass;
    private String login;
    private String uri;
    private LinkedTreeMap<String, String> sParams;
    private JSONObject jsonParams;
    private Callbacks callbacks;
    
    //------------------------------------------------------------------
    // Application level success and failure handling (not http failure)
    //------------------------------------------------------------------
    public static class ParamKeys{
        public static final String RESPONSE_STATUS = "status";
        public static final String ERROR_TITLE = "title";
        public static final String ERROR_MSG = "msg";
    }
    
    public static class StatusValues{
        public static final String STATUS_SUCCESS = "success";
        public static final String STATUS_FAILURE = "failure";
    }
    
    public static boolean isSuccess(String status){
        Log.i(TAG, "is isSuccess:" + status);
        // If no status parameter is given then assume success.
        if (status == null)
            return true;
        if (status.equals(StatusValues.STATUS_SUCCESS))
            return true;
        else
            return false;
    }

    public static boolean isFailure(String status){
        return !isSuccess(status);
    }
    
    //--------------
    // Instantiation
    //--------------
    public HttpRequest(String uri, Callbacks callbacks){
        this.uri = uri;
        sParams = null;
        method = "GET";
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq());
    }


    public HttpRequest(String uri, LinkedTreeMap<String, String> params, Callbacks callbacks){
        this.uri = uri;
        sParams = params;
        method = "GET";
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq());
    }

    public HttpRequest(String uri, LinkedTreeMap<String, String> params, String method){
        this.uri = uri;
        this.method = method;
        sParams = params;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq());
    }

    public HttpRequest(String uri, LinkedTreeMap<String, String> params, String method, Callbacks callbacks) {
        this.uri = uri;
        this.method = method;
        sParams = params;
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq());
    }

    public HttpRequest(String uri, LinkedTreeMap<String, String> params, String login, String pass, Callbacks callbacks){
        this.uri = uri;
        method = "GET";
        sParams = params;
        this.login = login;
        this.pass = pass;
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq());
    }

    public HttpRequest(String uri, JSONObject json, String method, Callbacks callbacks) {
        this.uri = uri;
        this.method = method;
        this.jsonParams = json;
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq());
    }

    //-----
    // Http
    //-----
    public String paramsToString(LinkedTreeMap<String, String> params){
        String s = "";
        for (String k: params.keySet()){
            s += Uri.encode(k) + "=" + Uri.encode(params.get(k)) + "&";
        }
        if (s.length() > 0 && s.charAt(s.length()-1)=='&') {
            s = s.substring(0, s.length()-1);
        }
        return s;
    }

    private boolean isPost(){
        return method.startsWith("POST") || method.startsWith("post");
    }

    public String httpReq() throws IOException{
        Logger.i(TAG, toString());
        String sUrl = Config.fullUrl(uri);
        String result = "";

        DefaultHttpClient http = new DefaultHttpClient();
        http.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Android");
        http.getParams().setParameter("Accept-Charset", "UTF-8");

        if(login==null || pass == null){
            User user = UserFactory.current_user();
            if(user != null){
                login = user.get(User.Attributes.MKEY);
                pass = user.get(User.Attributes.AUTH);
            }else{
                login = "";
                pass = "";
            }
        }

        http.getCredentialsProvider().setCredentials(
                new AuthScope(Config.getServerHost(), AuthScope.ANY_PORT, "zazo.com"),
                new UsernamePasswordCredentials(login, pass)
        );

        HttpUriRequest request;
        if (isPost()) {
            request = new HttpPost(sUrl);
            if (jsonParams != null) {
                Gson gson = new Gson();
                StringEntity entity = new StringEntity(jsonParams.toString());
                request.addHeader(HTTP.CONTENT_TYPE, "application/json");
                ((HttpPost)request).setEntity(entity);
            } else {
                List<NameValuePair> nameValuePairs = new LinkedList<NameValuePair>();
                for (String s : sParams.keySet()) {
                    nameValuePairs.add(new BasicNameValuePair(s, sParams.get(s)));
                }
                ((HttpPost)request).setEntity(new UrlEncodedFormEntity(nameValuePairs));
            }
        } else {
            if (sParams != null && sParams.size() > 0){
                sUrl+="?";
                List<NameValuePair> params = new LinkedList<NameValuePair>();
                for (String s : sParams.keySet()) {
                    params.add(new BasicNameValuePair(s, sParams.get(s)));
                }
                sUrl+= URLEncodedUtils.format(params, "utf-8");
            }
            request = new HttpGet(sUrl);
        }
        HttpResponse response = http.execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            Log.e(TAG, sUrl + "\n" + response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase());
            throw new IOException(response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase());
        }

        BufferedReader br = new BufferedReader( new InputStreamReader(/*con.getInputStream()*/response.getEntity().getContent()) );
        String l;
        while ( (l = br.readLine()) != null ){
            result += l;
        }
        br.close();

        return result;
    }

    protected void threadTaskDone(){    }

    private class BgHttpReq extends AsyncTask<Void, Void, HashMap<String, String>>{
        @Override
        protected HashMap<String, String> doInBackground(Void... params) {
            HashMap<String, String> r = new HashMap<String, String>();
            try {
                r.put("success", httpReq());
            } catch (MalformedURLException e) {
                r.put("error", e.toString());
            } catch (IOException e) {
                r.put("error", e.toString());
            }

            threadTaskDone();
            return r;
        }

        @Override
        protected void onPostExecute(HashMap<String, String> result) {
            if ( result.get("success") != null){
                if(callbacks!=null)
                    callbacks.success(result.get("success"));
            } else {
                String error = result.get("error");
                Log.d(TAG, error);
                if(callbacks!=null)
                    callbacks.error(error);
            }
        }

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(method).append(" ").append(uri);
        return builder.toString();
    }
}
