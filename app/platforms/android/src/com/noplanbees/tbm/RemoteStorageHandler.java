package com.noplanbees.tbm;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;


public abstract class RemoteStorageHandler {
	private final static String STAG = RemoteStorageHandler.class.getSimpleName();
	
	private RemoteStorageHandler rshInstance;
	
	public RemoteStorageHandler(){
		rshInstance = this;
	}
	
	public abstract void success(LinkedTreeMap<String, String> data);
	public abstract void error(LinkedTreeMap<String, String> data);

	//--------------------------------
	// Data structures keys and values
	//--------------------------------
	public static class DataKeys{
		public static String VIDEO_ID_KEY = "videoId";
		public static String STATUS_KEY = "status";
	}
	
	public static class STATUS_ENUM{
		public static String DOWNLOADED = "downloaded";
		public static String VIEWED = "viewed";
	}
	
	//------------------------
	// Keys for remote storage
	//------------------------
	public static String incomingVideoRemoteFilename(Friend friend) {
		return incomingConnectionKey(friend) + "-filename";
	}

	public static String outgoingVideoRemoteFilename(Friend friend) {
		return outgoingConnectionKey(friend) + "-filename";
	}

	public static String outgoingVideoIdRemoteKVKey(Friend friend) {
		return outgoingConnectionKey(friend) + "-VideoIdKVKey";
	}

	public static String incomingVideoIdRemoteKVKey(Friend friend) {
		return incomingConnectionKey(friend) + "-VideoIdKVKey";
	}

	public static String outgoingVideoStatusRemoteKVKey(Friend friend) {
		return outgoingConnectionKey(friend) + "-VideoStatusKVKey";
	}

	public static String incomingVideoStatusRemoteKVKey(Friend friend) {
		return incomingConnectionKey(friend) + "-VideoStatusKVKey";
	}
	
	private static String outgoingConnectionKey(Friend friend){
		return UserFactory.current_user().get(User.Attributes.MKEY) + "-" + friend.get(Friend.Attributes.MKEY);
	}
	
	private static String incomingConnectionKey(Friend friend){
		return friend.get(Friend.Attributes.MKEY) + "-" + UserFactory.current_user().get(User.Attributes.MKEY);
	}
	

	//------------
	// SetRemoteKV
	//------------
	public void setRemoteKV(String key, LinkedTreeMap<String, String>data){
		Gson g = new Gson();
		String value = g.toJson(data, data.getClass());
		
		LinkedTreeMap<String, String> params = new LinkedTreeMap<String, String>();
		params.put("key", key);
		params.put("value", value);
		new SetRemoteKV("kvstore/set", params, "POST");
	}
	
	private class SetRemoteKV extends Server{
		SetRemoteKV (String uri, LinkedTreeMap<String, String> params, String method){		
			super(uri, params, method);
		}
		@Override
		public void success(String response) {	
			Log.i(STAG, "SetRemoteKV: success");
			LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
			data.put("success", response);
			rshInstance.success(data);
		}
		@Override
		public void error(String errorString) {
			Log.e(STAG, "SetRemoteKV: ERROR: " + errorString);
			LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
			data.put("success", errorString);
			rshInstance.error(data);
		}
	}
	
	// Convenience setters
	public void setRemoteOutgoingVideoId(Friend friend, String videoId){
		LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
		data.put(RemoteStorageHandler.DataKeys.VIDEO_ID_KEY, videoId);
		setRemoteKV(RemoteStorageHandler.outgoingVideoIdRemoteKVKey(friend), data);
	}
	
	public void setRemoteIncomingVideoStatus(Friend friend, String videoId, String status){
		LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
		data.put(RemoteStorageHandler.DataKeys.VIDEO_ID_KEY, videoId);
		data.put(RemoteStorageHandler.DataKeys.STATUS_KEY, status);
		setRemoteKV(RemoteStorageHandler.outgoingVideoIdRemoteKVKey(friend), data);
	}
	
	//------------
	// GetRemoteKV
	//------------
	public void getRemoteKV(String key){
		new GetRemoteKV("kvstore/get?key=" + key);
	}
	
	private class GetRemoteKV extends Server{
		public GetRemoteKV(String uri) {
			super(uri);
		}
		@SuppressWarnings("unchecked")
		@Override
		public void success(String response) {
			Gson g = new Gson();
			LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
			data = g.fromJson(response, data.getClass());
			Log.i(STAG, "GetRemoteKV: success: " + data.toString());
			rshInstance.success(data);
		}
		@Override
		public void error(String errorString) {
			Log.e(STAG, "GetRemoteKV: ERROR: " + errorString);
			LinkedTreeMap<String, String> data = new LinkedTreeMap<String, String>();
			data.put("error", errorString);
			rshInstance.error(data);			
		}
	}
	
	
//	public static HashMap<String, String> senderAndReceiverIdsWithVideoId(String videoId){
//	HashMap <String, String> r = new HashMap<String, String>();
//	Pattern pattern = Pattern.compile("^(\\d+)-(\\d+)-");
//	Matcher matcher = pattern.matcher(videoId);
//	matcher.find();
//	
//	if (matcher.groupCount() != 2){
//		System.out.println("senderAndReceiverIdsWithVideoId: ERROR: Did not get 2 matches when searching for and reciever ids from video id. This should never happen.");
//		return null;
//	}
//	r.put("senderId", matcher.group(1));
//	r.put("receiverId", matcher.group(2));
//	return r;
//}

}
