package com.noplanbees.tbm.network;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.Config;
import com.noplanbees.tbm.model.User;
import com.noplanbees.tbm.model.UserFactory;
import com.noplanbees.tbm.utilities.AsyncTaskManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public abstract class Server {
    private static String STAG = Server.class.getSimpleName();
    private String pass;
    private String login;
    private String TAG = this.getClass().getSimpleName();
    private String uri;
    private String method;
    private LinkedTreeMap<String, String> sParams;

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
        Log.i(STAG, "is isSuccess:" + status);
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
    public Server(String uri, LinkedTreeMap<String, String> params,
                  String login, String pass){
        this.uri = uri;
        method = "GET";
        sParams = params;
        this.login = login;
        this.pass = pass;
        AsyncTaskManager.executeAsyncTask(new BgHttpReq(), new Void[]{});
    }

    public Server(String uri, LinkedTreeMap<String, String> params, String method){
        this.uri = uri;
        this.method = method;
        sParams = params;
        AsyncTaskManager.executeAsyncTask(new BgHttpReq(), new Void[]{});
    }

    public Server(String uri, LinkedTreeMap<String, String> params){
        this.uri = uri;
        sParams = params;
        method = "GET";
        AsyncTaskManager.executeAsyncTask(new BgHttpReq(), new Void[]{});
    }

    public Server(String uri){
        this.uri = uri;
        sParams = null;
        method = "GET";
        AsyncTaskManager.executeAsyncTask(new BgHttpReq(), new Void[]{});
    }

    //-----------------
    // Abstract methods
    //-----------------
    public abstract void success(String response);
    public abstract void error(String errorString);


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
                new AuthScope(Config.SERVER_HOST, AuthScope.ANY_PORT, "zazo.com"),
                new UsernamePasswordCredentials(login, pass)
        );

        HttpUriRequest request;
        if(isPost()){
            request = new HttpPost(sUrl);
            List<NameValuePair> nameValuePairs = new LinkedList<NameValuePair>();
            for (String s : sParams.keySet()) {
                nameValuePairs.add(new BasicNameValuePair(s, sParams.get(s)));
            }
            ((HttpPost)request).setEntity(new UrlEncodedFormEntity(nameValuePairs));

        }else{
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
        Log.i(TAG, "httpReq " + method + " url=" + sUrl +"  params=" + sParams);

        HttpResponse response = http.execute(request);

        Log.d(TAG, "response: " + response.getStatusLine().getStatusCode());


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
                success(result.get("success"));
            } else {
                String error = result.get("error");
                Log.d(TAG, error);
                error(error);
            }
        }

    }

}
