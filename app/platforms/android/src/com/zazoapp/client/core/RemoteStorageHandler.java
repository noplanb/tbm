package com.zazoapp.client.core;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.HttpRequest;
import com.zazoapp.client.utilities.StringUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;


public class RemoteStorageHandler {
    private static final String TAG = RemoteStorageHandler.class.getSimpleName();
    private static final String VIDEO_ID_KV_KEY = "-VideoIdKVKey";
    private static final String VIDEO_STATUS_KV_KEY = "-VideoStatusKVKey";

    //--------------------------------
    // Data structures keys and values
    //--------------------------------
    private static class DataKeys {
        public static final String VIDEO_ID_KEY = "videoId";
        public static final String STATUS_KEY = "status";
        public static final String VALUE_KEY = "value";
        public static final String KEY1_KEY = "key1";
        public static final String KEY2_KEY = "key2";
    }

    public static class StatusEnum {
        public static final String DOWNLOADED = "downloaded";
        public static final String VIEWED = "viewed";
    }

    //=======================================
    // Convenience Public setters and getters
    //=======================================

    //--------
    // Setters
    //--------
   public static void addRemoteOutgoingVideoId(Friend friend, String videoId){
        LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
        data.put(DataKeys.VIDEO_ID_KEY, videoId);
        setRemoteKV(outgoingVideoIdsRemoteKVKey(friend), videoId, data);
    }

    public static void setRemoteIncomingVideoStatus(Friend friend, String videoId, String status){
        LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
        data.put(DataKeys.VIDEO_ID_KEY, videoId);
        data.put(DataKeys.STATUS_KEY, status);
        setRemoteKV(incomingVideoStatusRemoteKVKey(friend), data);
    }

    //--------
    // Getters
    //--------
    // incomingVideoIds
    public static abstract class GetRemoteIncomingVideoIds extends GetRemoteKVs{
        private final Friend friend;
        
        public GetRemoteIncomingVideoIds(Friend friend){
            super(incomingVideoIdsRemoteKVKey(friend));
            this.friend = friend;
        }

        @Override
        public void gotRemoteKVs(ArrayList<LinkedTreeMap<String, String>> kvs) {    
            ArrayList<String>values = new ArrayList<String>();          
            for (LinkedTreeMap<String, String> kv : kvs){
                String vidJson = kv.get(DataKeys.VALUE_KEY);
                LinkedTreeMap<String, String> vidObj = StringUtils.linkedTreeMapWithJson(vidJson);
                if (vidObj != null) {
                    values.add(vidObj.get(DataKeys.VIDEO_ID_KEY));
                }
            }
            gotVideoIds(values);
        }
        protected abstract void gotVideoIds(ArrayList<String>videoIds);

        protected Friend getFriend() {
            return friend;
        }
    }

    // OutgoingVideoStatus
    public static abstract class GetRemoteOutgoingVideoStatus extends GetRemoteKV{
        private final Friend friend;

        public GetRemoteOutgoingVideoStatus(Friend friend){
            super(outgoingVideoStatusRemoteKVKey(friend), null);
            this.friend = friend;
        }

        @Override
        protected void gotRemoteKV(LinkedTreeMap<String, String> kv) {
            if (kv == null)
                gotVideoIdStatus(null, null);
            else
                gotVideoIdStatus(kv.get(DataKeys.VIDEO_ID_KEY), kv.get(DataKeys.STATUS_KEY));
        }

        protected abstract void gotVideoIdStatus(String videoId, String status);

        protected Friend getFriend() {
            return friend;
        }
    }

    
    //-------
    // Delete
    //-------
    public static void deleteRemoteIncomingVideoId(Friend friend, String videoId){
        deleteRemoteKV(incomingVideoIdsRemoteKVKey(friend), videoId);
    }
	
	//------------------------
	// Keys for remote storage
	//------------------------
	public static String outgoingVideoRemoteFilename(Friend friend, String videoId) {
		return buildOutgoingVideoFilenameKey(friend, videoId);
	}

	public static String incomingVideoRemoteFilename(Friend friend, String videoId) {
		return buildIncomingVideoFilenameKey(friend, videoId);
	}

	private static String outgoingVideoIdsRemoteKVKey(Friend friend) {
		return buildOutgoingKvKey(friend, VIDEO_ID_KV_KEY);
	}

	private static String incomingVideoIdsRemoteKVKey(Friend friend) {
		return buildIncomingKvKey(friend, VIDEO_ID_KV_KEY);
	}

	private static String outgoingVideoStatusRemoteKVKey(Friend friend) {
		return buildOutgoingKvKey(friend, VIDEO_STATUS_KV_KEY);
	}

	private static String incomingVideoStatusRemoteKVKey(Friend friend) {
		return buildIncomingKvKey(friend, VIDEO_STATUS_KV_KEY);
	}

    public static String buildIncomingVideoFilenameKey(Friend friend, String videoId) {
        StringBuilder builder = new StringBuilder();
        builder.append(friend.get(Friend.Attributes.MKEY)).append("-");
        builder.append(UserFactory.current_user().get(User.Attributes.MKEY)).append("-");
        builder.append(md5(friend.get(Friend.Attributes.CKEY) + videoId));
        return builder.toString();
    }

    public static String buildOutgoingVideoFilenameKey(Friend friend, String videoId) {
        StringBuilder builder = new StringBuilder();
        builder.append(UserFactory.current_user().get(User.Attributes.MKEY)).append("-");
        builder.append(friend.get(Friend.Attributes.MKEY)).append("-");
        builder.append(md5(friend.get(Friend.Attributes.CKEY) + videoId));
        return builder.toString();
    }

    public static String buildIncomingKvKey(Friend friend, String suffix) {
        StringBuilder builder = new StringBuilder();
        String sender = friend.get(Friend.Attributes.MKEY);
        String receiver = UserFactory.current_user().get(User.Attributes.MKEY);
        String ckey = friend.get(Friend.Attributes.CKEY);
        builder.append(sender).append("-").append(receiver).append("-");
        builder.append(md5(sender + receiver + ckey));
        builder.append(suffix);
        return builder.toString();
    }

    public static String buildOutgoingKvKey(Friend friend, String suffix) {
        StringBuilder builder = new StringBuilder();
        String sender = UserFactory.current_user().get(User.Attributes.MKEY);
        String receiver = friend.get(Friend.Attributes.MKEY);
        String ckey = friend.get(Friend.Attributes.CKEY);
        builder.append(sender).append("-").append(receiver).append("-");
        builder.append(md5(sender + receiver + ckey));
        builder.append(suffix);
        return builder.toString();
    }
	//--------------------------
	// Set Get and Delete Remote
	//--------------------------
	
	// Set key
	private static void setRemoteKV(String key, LinkedTreeMap<String, String>data){
		String key2 = null;
		setRemoteKV(key, key2, data);
	}
	
	// Set key1, key2
	private static void setRemoteKV(String key1, String key2, LinkedTreeMap<String, String>data){
		Gson g = new Gson();
		String value = g.toJson(data, data.getClass());
		
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put(DataKeys.KEY1_KEY, key1);
		if (key2 != null)
			params.put(DataKeys.KEY2_KEY, key2);
		params.put(DataKeys.VALUE_KEY, value);
		new SetRemote("kvstore/set", params, "POST");
	}
	
	private static class SetRemote extends HttpRequest {
		public SetRemote (String uri, LinkedTreeMap<String, String> params, String method){		
			super(uri, params, method, new Callbacks() {
                @Override
                public void success(String response) {
                    LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
                    data.put("success", response);
                }

                @Override
                public void error(String errorString) {
                    Log.e(TAG, "SetRemote: ERROR: " + errorString);
                }
            });
		}
	}

	// Get key1, key2
	private static abstract class GetRemoteKV{

	    public GetRemoteKV(String key1, String key2){
	        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
	        params.put(DataKeys.KEY1_KEY, key1);
	        if (key2 != null)
	            params.put(DataKeys.KEY2_KEY, key2);
	        new GetRemoteKVRequest("kvstore/get", params, "GET");
	    }

	    protected abstract void gotRemoteKV(LinkedTreeMap<String, String>kv);

	    private class GetRemoteKVRequest extends HttpRequest{
	        public GetRemoteKVRequest(String uri, LinkedTreeMap<String, String> params, String method) {
	            super(uri, params, method, new Callbacks() {

	                @SuppressWarnings("unchecked")
	                @Override
	                public void success(String response) {

                        if (response.isEmpty()){
                            gotRemoteKV(null);
                            return;
                        }

                        LinkedTreeMap<String, String> data = StringUtils.linkedTreeMapWithJson(response);
                        if (data != null) {
                            gotRemoteKV(StringUtils.linkedTreeMapWithJson(data.get(DataKeys.VALUE_KEY)));
                        } else {
                            gotRemoteKV(null);
                        }
                    }

	                @Override
	                public void error(String errorString) {
	                    Log.e(TAG, "GetRemoteKV: " + errorString);
	                }
	            });
	        }
	    }
	}
	
	// Get all
	private static abstract class GetRemoteKVs{
		public GetRemoteKVs(String key1){
			LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
			params.put(DataKeys.KEY1_KEY, key1);
			new GetRemoteKVsRequest("kvstore/get_all", params, "GET");
		}
		
		protected abstract void gotRemoteKVs(ArrayList<LinkedTreeMap<String, String>> kvs);
		
		private class GetRemoteKVsRequest extends HttpRequest{			
			public GetRemoteKVsRequest(String uri, LinkedTreeMap<String, String> params, String method) {
				super(uri, params, method, new Callbacks() {
                    @Override
                    public void success(String response) {
                        Gson g = new Gson();
                        ArrayList<LinkedTreeMap<String, String>> kvs = null;
                        try {
                            kvs = g.fromJson(response, ArrayList.class);
                            gotRemoteKVs(kvs);
                        } catch (JsonSyntaxException e) {
                            error("JsonSyntaxException");
                        }
                    }

                    @Override
                    public void error(String errorString) {
                        Log.e(TAG, "GetRemoteKVs: " + errorString);
                    }
                });
			}
		}
	}

    //-----------------
    // DeleteRemoteKV
    //-----------------
    private static void deleteRemoteKV(String key1, String key2){
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put(DataKeys.KEY1_KEY, key1);
        if (key2 != null)
            params.put(DataKeys.KEY2_KEY, key2);
        new HttpRequest("kvstore/delete", params, "GET", null);
    }

    private static String md5(String data) {
        return new String(Hex.encodeHex(DigestUtils.md5(data)));
    }
}
