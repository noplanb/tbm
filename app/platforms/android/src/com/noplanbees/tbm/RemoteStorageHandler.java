package com.noplanbees.tbm;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.dispatch.Dispatch;
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
	}
	
	public static class StatusEnum{
		public static String DOWNLOADED = "downloaded";
		public static String VIEWED = "viewed";
	}
	
	
    //---------------------------
	// Public setters and getters
    //---------------------------
	
	// Setters
    public static void addRemoteOutgoingVideoId(Friend friend, String videoId){
        LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
        data.put(RemoteStorageHandler.DataKeys.VIDEO_ID_KEY, videoId);
        setRemoteKV(RemoteStorageHandler.outgoingVideoIdsRemoteKVKey(friend), videoId, data);
    }

    
    public static void setRemoteIncomingVideoStatus(Friend friend, String videoId, String status){
        LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
        data.put(RemoteStorageHandler.DataKeys.VIDEO_ID_KEY, videoId);
        data.put(RemoteStorageHandler.DataKeys.STATUS_KEY, status);
        setRemoteKV(RemoteStorageHandler.incomingVideoStatusRemoteKVKey(friend), data);
    }
    
    // Getters
    public static abstract class GetRemoteIncomingVideoIds extends GetRemoteKVs{
        protected Friend friend;
        
        public GetRemoteIncomingVideoIds(Friend friend){
            super(incomingVideoIdsRemoteKVKey(friend));
            this.friend = friend;
        }

        @Override
        public void gotRemoteKVs(ArrayList<LinkedTreeMap<String, String>> kvs) {    
            Log.i(TAG, "gotRemoteKVs: ");
            ArrayList<String>values = new ArrayList<String>();          
            for (LinkedTreeMap<String, String> kv : kvs){
                String vidJson = kv.get("value");
                Gson g = new Gson();
                LinkedTreeMap<String, String> vidObj = new LinkedTreeMap<String, String>();
                vidObj = g.fromJson(vidJson, vidObj.getClass());
                values.add(vidObj.get(DataKeys.VIDEO_ID_KEY));
            }
            gotVideoIds(values);
        }
        public abstract void gotVideoIds(ArrayList<String>videoIds);
    }


    public static void getRemoteOutgoingVideoStatus(final Friend friend){
        getRemoteKV(outgoingConnectionKey(friend) + "-VideoStatusKVKey", null, new HttpRequest.Callbacks() {
            @Override
            public void success(String response) {
                Gson g = new Gson();
                LinkedTreeMap<String, String> data;
                data = g.fromJson(response, LinkedTreeMap.class);
                Log.d(TAG, "getRemoteOutgoingVideoStatus: " + data);
                if (data != null && data.get("value") != null) {
                    LinkedTreeMap<String, String> value = g.fromJson(data.get("value"), LinkedTreeMap.class);
                    Log.d(TAG, "getRemoteOutgoingVideoStatus: " + value);
                    String videoId = value.get("videoId");
                    String status = value.get("status");
                    if(status.equals(StatusEnum.DOWNLOADED))
                        friend.setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.DOWNLOADED);
                    else if(status.equals(StatusEnum.VIEWED))
                        friend.setAndNotifyOutgoingVideoStatus(Friend.OutgoingVideoStatus.VIEWED);
                }
            }

            @Override
            public void error(String errorString) {
                Log.d(TAG, "GetRemoteKV: ERROR: " + errorString);
                LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
                data.put("error", errorString);
            }
        });
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
	

	//--------------------
	// SetOrDeleteRemote
	//--------------------
	private static void setRemoteKV(String key, LinkedTreeMap<String, String>data){
		String key2 = null;
		setRemoteKV(key, key2, data);
	}
	
	private static void setRemoteKV(String key1, String key2, LinkedTreeMap<String, String>data){
		Gson g = new Gson();
		String value = g.toJson(data, data.getClass());
		
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put("key1", key1);
		if (key2 != null)
			params.put("key2", key2);
		params.put("value", value);
		new SetRemote("kvstore/set", params, "POST");
	}
	
	private static class SetRemote extends HttpRequest {
		public SetRemote (String uri, LinkedTreeMap<String, String> params, String method){		
			super(uri, params, method, new Callbacks() {
                @Override
                public void success(String response) {
                    Log.i(TAG, "SetOrDeleteRemote: success");
                    LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
                    data.put("success", response);
                }

                @Override
                public void error(String errorString) {
                    Log.d(TAG, "SetOrDeleteRemote: ERROR: " + errorString);
                    LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
                    data.put("success", errorString);
                }
            });
		}
	}

    private static void getRemoteKV(String key1, String key2, HttpRequest.Callbacks callbacks){
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put("key1", key1);
        if (key2 != null)
            params.put("key2", key2);
        new GetRemoteKV("kvstore/get", params, "GET", callbacks);
    }
	
	private static class GetRemoteKV extends HttpRequest{
        public GetRemoteKV(String uri, LinkedTreeMap<String, String> params, String method) {
            super(uri, params, method);
        }
        public GetRemoteKV(String uri, LinkedTreeMap<String, String> params, String method, Callbacks callbacks) {
            super(uri, params, method, callbacks);
        }
	}
	
	private static abstract class GetRemoteKVs{
		public GetRemoteKVs(String key1){
			LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
			params.put("key1", key1);
			new GetRemoteKVsServer("kvstore/get_all", params, "GET");
		}
		
		protected abstract void gotRemoteKVs(ArrayList<LinkedTreeMap<String, String>> kvs);
		
		private class GetRemoteKVsServer extends HttpRequest{			
			public GetRemoteKVsServer(String uri, LinkedTreeMap<String, String> params, String method) {
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
        params.put("key1", key1);
        if (key2 != null)
            params.put("key2", key2);
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
