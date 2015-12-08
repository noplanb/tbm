package com.zazoapp.client.network;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.utilities.AsyncTaskManager;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class HttpRequest {

    public static interface Callbacks{
        void success(String response);
        void error(String errorString);
    }
    
    private String method;

    private static final String TAG = HttpRequest.class.getSimpleName();
    private String pass;
    private String login;
    private String uri;
    private String url;
    private String host;
    private boolean securedConnection;
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
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq(), new Void[]{});
    }


    public HttpRequest(String uri, LinkedTreeMap<String, String> params, Callbacks callbacks){
        this.uri = uri;
        sParams = params;
        method = "GET";
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq(), new Void[]{});
    }

    public HttpRequest(String uri, LinkedTreeMap<String, String> params, String method){
        this.uri = uri;
        this.method = method;
        sParams = params;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq(), new Void[]{});
    }

    public HttpRequest(String uri, LinkedTreeMap<String, String> params, String method, Callbacks callbacks) {
        this.uri = uri;
        this.method = method;
        sParams = params;
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq(), new Void[]{});
    }

    public HttpRequest(String uri, LinkedTreeMap<String, String> params, String login, String pass, Callbacks callbacks){
        this.uri = uri;
        method = "GET";
        sParams = params;
        this.login = login;
        this.pass = pass;
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq(), new Void[]{});
    }

    public HttpRequest(String uri, JSONObject json, String method, Callbacks callbacks) {
        this.uri = uri;
        this.method = method;
        this.jsonParams = json;
        this.callbacks = callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq(), new Void[]{});
    }

    private HttpRequest(Builder builder) {
        if (builder.url == null) {
            url = Config.fullUrl(builder.uri);
            host = Config.getServerHost();
        } else {
            url = builder.url;
            host = builder.host;
        }
        securedConnection = url.toLowerCase().startsWith("https:");
        method = (builder.method != null) ? builder.method : GET;
        sParams = builder.params;
        login = builder.login;
        pass = builder.pass;
        jsonParams = builder.jsonParams;
        callbacks = builder.callbacks;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq());
    }

    public static final String POST = "POST";
    public static final String GET = "GET";

    public static class Builder {
        private String url;
        private String uri;
        private String method;
        private LinkedTreeMap<String, String> params;
        private String login;
        private String pass;
        private Callbacks callbacks;
        private JSONObject jsonParams;
        private String host;

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setUri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setParams(LinkedTreeMap<String, String> params) {
            this.params = params;
            return this;
        }

        public Builder setLogin(String login) {
            this.login = login;
            return this;
        }

        public Builder setPass(String pass) {
            this.pass = pass;
            return this;
        }

        public Builder setCallbacks(Callbacks callbacks) {
            this.callbacks = callbacks;
            return this;
        }

        public Builder setJsonParams(JSONObject jsonParams) {
            this.jsonParams = jsonParams;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public HttpRequest build() {
            if (TextUtils.isEmpty(url) && TextUtils.isEmpty(uri)) {
                throw new IllegalStateException("At least one from url or uri must be set");
            }
            if (this.jsonParams != null && this.params != null) {
                throw new IllegalStateException("Both json and non-json params can't be used at once");
            }
            if (!TextUtils.isEmpty(url) && TextUtils.isEmpty(this.host)) {
                throw new IllegalStateException("Host must be set if url is set");
            }
            return new HttpRequest(this);
        }
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
        String sUrl = (url == null) ? Config.fullUrl(uri) : url;
        String sHost = (host == null) ? Config.getServerHost() : host;
        String result = "";

        DefaultHttpClient http = new DefaultHttpClient();
        http.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Android");
        http.getParams().setParameter("Accept-Charset", "UTF-8");
        if (securedConnection) {
            SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("https",
                    SSLSocketFactory.getSocketFactory(), 443));
            SingleClientConnManager mgr = new SingleClientConnManager(http.getParams(), schemeRegistry);
            http = new DefaultHttpClient(mgr, http.getParams());
        }
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
                new AuthScope(sHost, AuthScope.ANY_PORT, "zazo.com"),
                new UsernamePasswordCredentials(login, pass)
        );

        HttpUriRequest request;
        if (isPost()) {
            request = new HttpPost(sUrl);
            if (jsonParams != null) {
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

}