package com.noplanbees.tbm;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.User;
import com.noplanbees.tbm.model.UserFactory;
import com.noplanbees.tbm.network.HttpRequest;

import java.util.ArrayList;


public class RemoteStorageHandler {
	private final static String TAG = RemoteStorageHandler.class.getSimpleName();

	//--------------------------------
	// Data structures keys and values
	//--------------------------------
	private static class DataKeys{
		public static String VIDEO_ID_KEY = "videoId";
		public static String STATUS_KEY = "status";
		public static String VALUE_KEY = "value";
		public static String KEY1_KEY = "key1";
		public static String KEY2_KEY = "key2";
	}
	
	public static class StatusEnum{
		public static String DOWNLOADED = "downloaded";
		public static String VIEWED = "viewed";
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
                Gson g = new Gson();
                LinkedTreeMap<String, String> vidObj = new LinkedTreeMap<String, String>();
                vidObj = g.fromJson(vidJson, vidObj.getClass());
                values.add(vidObj.get(DataKeys.VIDEO_ID_KEY));
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
	public static String outgoingVideoRemoteFilename(Friend friend) {
		return outgoingConnectionKey(friend) + "-" + friend.get(Friend.Attributes.OUTGOING_VIDEO_ID) + "-filename";
	}

	public static String incomingVideoRemoteFilename(Friend friend, String videoId) {
		return incomingConnectionKey(friend) + "-" + videoId + "-filename";
	}

	private static String outgoingVideoIdsRemoteKVKey(Friend friend) {
		return outgoingConnectionKey(friend) + "-VideoIdKVKey";
	}

	private static String incomingVideoIdsRemoteKVKey(Friend friend) {
		return incomingConnectionKey(friend) + "-VideoIdKVKey";
	}

	private static String outgoingVideoStatusRemoteKVKey(Friend friend) {
		return outgoingConnectionKey(friend) + "-VideoStatusKVKey";
	}

	private static String incomingVideoStatusRemoteKVKey(Friend friend) {
		return incomingConnectionKey(friend) + "-VideoStatusKVKey";
	}
	
	private static String outgoingConnectionKey(Friend friend){
		return UserFactory.current_user().get(User.Attributes.MKEY) + "-" + friend.get(Friend.Attributes.MKEY)
                + "-" + friend.get(Friend.Attributes.CKEY);
	}
	
	private static String incomingConnectionKey(Friend friend){
		return friend.get(Friend.Attributes.MKEY) + "-" + UserFactory.current_user().get(User.Attributes.MKEY)
                + "-" + friend.get(Friend.Attributes.CKEY);
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
	                    LinkedTreeMap<String, String> data;
	                    
	                    if (response.isEmpty()){
	                        gotRemoteKV(null);
	                        return;
	                    }

                        Gson gson = new Gson();
                        data = gson.fromJson(response, LinkedTreeMap.class);
                        if(data!=null) {
                            LinkedTreeMap<String, String> value;
                            value = gson.fromJson(data.get(DataKeys.VALUE_KEY), LinkedTreeMap.class);
                            gotRemoteKV(value);
                        }else
                            gotRemoteKV(null);
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
                        ArrayList<LinkedTreeMap<String, String>> kvs = new ArrayList<LinkedTreeMap<String, String>>();
                        kvs = g.fromJson(response, kvs.getClass());
                        gotRemoteKVs(kvs);
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
        new DeleteRemote("kvstore/delete", params, "GET");
    }

    private static class DeleteRemote extends HttpRequest{
        public DeleteRemote (String uri, LinkedTreeMap<String, String> params, String method){
            super(uri, params, method, new Callbacks() {
                @Override
                public void success(String response) {
                }
                @Override
                public void error(String errorString) {
                }
            });
        }
    }
}
