package com.noplanbees.tbm;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.noplanbees.tbm.dispatch.Dispatch;
import com.noplanbees.tbm.model.Friend;
import com.noplanbees.tbm.model.User;
import com.noplanbees.tbm.model.UserFactory;
import com.noplanbees.tbm.network.Server;

import java.util.ArrayList;


public class RemoteStorageHandler {
	private final static String STAG = RemoteStorageHandler.class.getSimpleName();
	
	public RemoteStorageHandler(){
	}

	//--------------------------------
	// Data structures keys and values
	//--------------------------------
	public static class DataKeys{
		public static String VIDEO_ID_KEY = "videoId";
		public static String STATUS_KEY = "status";
	}
	
	public static class StatusEnum{
		public static String DOWNLOADED = "downloaded";
		public static String VIEWED = "viewed";
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

	public static String outgoingVideoIdsRemoteKVKey(Friend friend) {
		return outgoingConnectionKey(friend) + "-VideoIdKVKey";
	}

	public static String incomingVideoIdsRemoteKVKey(Friend friend) {
		return incomingConnectionKey(friend) + "-VideoIdKVKey";
	}

	public static String outgoingVideoStatusRemoteKVKey(Friend friend) {
		return outgoingConnectionKey(friend) + "-VideoStatusKVKey";
	}

	public static String incomingVideoStatusRemoteKVKey(Friend friend) {
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
	public void setRemoteKV(String key, LinkedTreeMap<String, String>data){
		String key2 = null;
		setRemoteKV(key, key2, data);
	}
	
	public void setRemoteKV(String key1, String key2, LinkedTreeMap<String, String>data){
		Gson g = new Gson();
		String value = g.toJson(data, data.getClass());
		
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put("key1", key1);
		if (key2 != null)
			params.put("key2", key2);
		params.put("value", value);
		new SetRemote("kvstore/set", params, "POST");
	}
	
	private class SetRemote extends Server {
		public SetRemote (String uri, LinkedTreeMap<String, String> params, String method){		
			super(uri, params, method);
		}
		@Override
		public void success(String response) {	
			Log.i(STAG, "SetOrDeleteRemote: success");
			LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
			data.put("success", response);
		}
		@Override
		public void error(String errorString) {
			Log.d(STAG, "SetOrDeleteRemote: ERROR: " + errorString);
			LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
			data.put("success", errorString);
		}
	}
	
	// Convenience setters
	public void addRemoteOutgoingVideoId(Friend friend, String videoId){
		LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
		data.put(RemoteStorageHandler.DataKeys.VIDEO_ID_KEY, videoId);
		setRemoteKV(RemoteStorageHandler.outgoingVideoIdsRemoteKVKey(friend), videoId, data);
	}

	
	public void setRemoteIncomingVideoStatus(Friend friend, String videoId, String status){
		LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
		data.put(RemoteStorageHandler.DataKeys.VIDEO_ID_KEY, videoId);
		data.put(RemoteStorageHandler.DataKeys.STATUS_KEY, status);
		setRemoteKV(RemoteStorageHandler.incomingVideoStatusRemoteKVKey(friend), data);
	}
	
	// Convenience getters
	public abstract class GetRemoteIncomingVideoIds extends GetRemoteKVs{
		protected Friend friend;
		
		public GetRemoteIncomingVideoIds(Friend friend){
			super(incomingVideoIdsRemoteKVKey(friend));
			this.friend = friend;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void gotRemoteKVs(ArrayList<LinkedTreeMap<String, String>> kvs) {	
			Log.i(STAG, "gotRemoteKVs: ");
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
	
	
	//------------
	// GetRemoteKV
	//------------
	public void getRemoteKV(String key1, String key2){
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put("key1", key1);
		if (key2 != null)
			params.put("key2", key2);
		new GetRemoteKV("kvstore/get", params, "GET");
	}
	
	private class GetRemoteKV extends Server{
	
		public GetRemoteKV(String uri, LinkedTreeMap<String, String> params, String method) {
			super(uri, params, method);
		}
		@SuppressWarnings("unchecked")
		@Override
		public void success(String response) {
			Gson g = new Gson();
			LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
			data = g.fromJson(response, data.getClass());
			Log.i(STAG, "GetRemoteKV: success: " + data.toString());
		}
		@Override
		public void error(String errorString) {
			Log.d(STAG, "GetRemoteKV: ERROR: " + errorString);
			LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
			data.put("error", errorString);
		}
	}
	
	private abstract class GetRemoteKVs{
		public GetRemoteKVs(String key1){
			LinkedTreeMap<String, String>params = new LinkedTreeMap<String, String>();
			params.put("key1", key1);
			new GetRemoteKVsServer("kvstore/get_all", params, "GET");
		}
		
		public abstract void gotRemoteKVs(ArrayList<LinkedTreeMap<String, String>> kvs);
		
		private class GetRemoteKVsServer extends Server{
			private String TAG = this.getClass().getSimpleName();
			
			public GetRemoteKVsServer(String uri, LinkedTreeMap<String, String> params, String method) {
				super(uri, params, method);
			}
			@SuppressWarnings("unchecked")
			@Override
			public void success(String response) {	
				Gson g = new Gson();
				ArrayList<LinkedTreeMap<String, String>> kvs = new ArrayList<LinkedTreeMap<String, String>>();
				kvs = g.fromJson(response, kvs.getClass());
				gotRemoteKVs(kvs);
			}
			@Override
			public void error(String errorString) {	
				Dispatch.dispatch("GetRemoteKVs: " + errorString);
			}
		}
	}



    //-----------------
    // DeleteRemoteFile
    //-----------------
	
    public static void deleteRemoteKV(String key1, String key2){
        LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
        params.put("key1", key1);
        if (key2 != null)
            params.put("key2", key2);
        new DeleteRemote("kvstore/delete", params, "GET");
    }

    private static class DeleteRemote extends Server{
        public DeleteRemote (String uri, LinkedTreeMap<String, String> params, String method){
            super(uri, params, method);
        }
        @Override
        public void success(String response) {
        }
        @Override
        public void error(String errorString) {
        }
    }
}
