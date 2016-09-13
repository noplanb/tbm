package com.zazoapp.client.network;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.utilities.AsyncTaskManager;
import com.zazoapp.client.utilities.Logger;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.auth.AuthScope;
import cz.msebera.android.httpclient.auth.UsernamePasswordCredentials;
import cz.msebera.android.httpclient.client.entity.EntityBuilder;
import cz.msebera.android.httpclient.client.methods.HttpDelete;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPatch;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.RequestBuilder;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;
import cz.msebera.android.httpclient.conn.scheme.Scheme;
import cz.msebera.android.httpclient.conn.scheme.SchemeRegistry;
import cz.msebera.android.httpclient.conn.ssl.SSLSocketFactory;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.mime.MultipartEntityBuilder;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.impl.conn.SingleClientConnManager;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.params.CoreProtocolPNames;
import cz.msebera.android.httpclient.params.HttpConnectionParams;
import cz.msebera.android.httpclient.protocol.HTTP;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
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
    private String url;
    private String host;
    private boolean securedConnection;
    private LinkedTreeMap<String, String> sParams;
    private JSONObject jsonParams;
    private Callbacks callbacks;
    private int timeout;
    private boolean multipart;
    private String filePath;
    
    //------------------------------------------------------------------
    // Application level success and failure handling (not http failure)
    //------------------------------------------------------------------
    public static class ParamKeys{
        public static final String RESPONSE_STATUS = "status";
        public static final String ERROR_TITLE = "title";
        public static final String ERROR_MSG = "msg";
    }

    public static class ServerResponse {
        @SerializedName(ParamKeys.RESPONSE_STATUS) public String status;
        @SerializedName(ParamKeys.ERROR_TITLE) public String title;
        @SerializedName(ParamKeys.ERROR_MSG) public String message;
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

    private static String DEVICE_INFO = "";
    static {
        try {
            DEVICE_INFO += Build.MANUFACTURER;
        } catch (RuntimeException e) {
        }
        DEVICE_INFO += "\n";
        try {
            DEVICE_INFO += Build.MODEL;
        } catch (RuntimeException e) {
        }
        DEVICE_INFO += "\n";
        try {
            DEVICE_INFO += Build.VERSION.RELEASE;
        } catch (RuntimeException e) {
        }
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
        timeout = builder.timeout;
        filePath = builder.filePath;
        multipart = builder.multipart;
        AsyncTaskManager.executeAsyncTask(true, new BgHttpReq());
    }

    public static final String POST = HttpPost.METHOD_NAME;
    public static final String GET = HttpGet.METHOD_NAME;
    public static final String CREATE = POST;
    public static final String READ = GET;
    public static final String UPDATE = HttpPatch.METHOD_NAME;
    public static final String DELETE = HttpDelete.METHOD_NAME;

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
        private int timeout = -1;
        private boolean multipart;
        private String filePath;

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

        public Builder setFilepath(String filepath) {
            this.filePath = filepath;
            return this;
        }

        /**
         *
         * @param timeout in seconds
         * @return
         */
        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
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
            multipart = jsonParams != null && filePath != null;
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
        return POST.equalsIgnoreCase(method);
    }

    private boolean isPatch(){
        return UPDATE.equalsIgnoreCase(method);
    }

    private boolean isDelete(){
        return DELETE.equalsIgnoreCase(method);
    }

    public String httpReq() throws IOException{
        Logger.i(TAG, toString());
        String sUrl = (url == null) ? Config.fullUrl(uri) : url;
        String sHost = (host == null) ? Config.getServerHost() : host;
        String result = "";
        DefaultHttpClient http = new DefaultHttpClient();
        http.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Android");
        http.getParams().setParameter("Accept-Charset", "UTF-8");
        if (timeout > 0) {
            HttpConnectionParams.setConnectionTimeout(http.getParams(), timeout * 1000);
            HttpConnectionParams.setSoTimeout(http.getParams(), timeout *1000);
        }
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

        RequestBuilder rb;
        if (isPost() || isDelete() || isPatch()) {
            rb = RequestBuilder.create(method);
            if (multipart) {
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                entityBuilder.addTextBody("json", jsonParams.toString(), ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), "UTF-8"));
                entityBuilder.addBinaryBody("file", new File(filePath));
            } else if (jsonParams != null) {
                rb.setEntity(EntityBuilder.create()
                        .setText(jsonParams.toString())
                        .setContentType(ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), "UTF-8")).build());
                rb.addHeader(HTTP.CONTENT_TYPE, "application/json");
            } else if (sParams != null && sParams.size() > 0) {
                for (String s : sParams.keySet()) {
                    rb.addParameter(s, sParams.get(s));
                }
            }
        } else {
            rb = RequestBuilder.get();
            if (sParams != null && sParams.size() > 0) {
                sUrl+="?";
                List<NameValuePair> params = new LinkedList<NameValuePair>();
                for (String s : sParams.keySet()) {
                    params.add(new BasicNameValuePair(s, sParams.get(s)));
                }
                sUrl+= URLEncodedUtils.format(params, "utf-8");
            }
        }
        rb.setUri(sUrl);
        rb.addHeader("Device-Platform", "android");
        rb.addHeader("App-Version", TbmApplication.getVersionNumber());
        rb.addHeader("Device-Info", DEVICE_INFO);
        HttpResponse response = http.execute(rb.build());
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            BufferedReader br = new BufferedReader( new InputStreamReader(/*con.getInputStream()*/entity.getContent()));
            String l;
            while ( (l = br.readLine()) != null ){
                result += l;
            }
            br.close();
        }
        if (response.getStatusLine().getStatusCode() != 200) {
            String answer = (result.isEmpty()) ? "" : " respond: " + result;
            Logger.e(TAG, sUrl + "\n" + response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase() + answer);
            throw new IOException(response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase() + answer);
        }

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
