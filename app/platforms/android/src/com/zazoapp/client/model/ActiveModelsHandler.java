package com.zazoapp.client.model;

import android.content.Context;
import android.util.Log;
import com.zazoapp.client.PreferencesHelper;

public class ActiveModelsHandler {
    public static final String USER_REGISTERED = "user_registered";
	private static final String TAG = ActiveModelsHandler.class.getSimpleName();
	
	private static ActiveModelsHandler instance;
	
	private UserFactory uf;
	private FriendFactory ff;
	private VideoFactory vf;
	private GridElementFactory gf;
	
	private Context context;

	private ActiveModelsHandler(Context context) {
		this.context = context;
	}

    public static ActiveModelsHandler getInstance(Context context) {
        ActiveModelsHandler localInstance = instance;
        if (localInstance != null) {
            if (!context.getApplicationContext().equals(localInstance.context)) {
                localInstance = null;
            }
        }
        if (localInstance == null) {
            synchronized (ActiveModelsHandler.class) {
                localInstance = instance;
                if (localInstance == null) {
                    localInstance = new ActiveModelsHandler(context.getApplicationContext());
                    instance = localInstance;
                }
            }
        }
        return instance;
    }

    public void ensureAll(){
		ensureUser();
        ensureFriend();
        ensureVideo();
        ensureGridElement();
        Log.d(TAG, "ensureAll end");
    }

	public void saveAll(){
		saveUser();
		saveFriend();
        Log.i(TAG, "saveAll: saving " + VideoFactory.getFactoryInstance().count() + " videos");
        saveVideo();
        saveGridElement();
        new PreferencesHelper(context).putBoolean(USER_REGISTERED, User.isRegistered(context));
        Log.i(TAG, "saveAll end");
    }
	
	public void retrieveAll(){
		retrieveUser();
		retrieveFriend();
        retrieveVideo();
        Log.i(TAG, "retrieveAll: retrieved " + VideoFactory.getFactoryInstance().count() + "videos");
        retrieveGridElement();
    }

    public void destroyAll() {
        UserFactory.getFactoryInstance().destroyAll(context);
        FriendFactory.getFactoryInstance().destroyAll(context);
        VideoFactory.getFactoryInstance().destroyAll(context);
        GridElementFactory.getFactoryInstance().destroyAll(context);
    }

    public UserFactory ensureUser(){
		uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "User present in memory.");
		} else if (uf.retrieve(context)){
			Log.i(TAG, "Retrieved User from local storage.");
		} else {
			Log.i(TAG, "User not retrievable from local storage. Creating an instance.");
            uf.makeInstance(context);
		}
		return uf;
	}
	
	public FriendFactory ensureFriend(){
		ff = FriendFactory.getFactoryInstance();
		FriendFactory r;
		if (ff.hasInstances()){
			Log.i(TAG, "Friend present in memory");
			r = ff;
		} else if (ff.retrieve(context)){
			Log.i(TAG, "Retrieved Friend from local storage.");
			r = ff;
		} else {
            Log.d(TAG, "Friend not retrievable from local storage.");
            r = null;
		}
		return r;
	}
	
	public VideoFactory ensureVideo(){
		vf = VideoFactory.getFactoryInstance();
		VideoFactory r;
		if (vf.hasInstances()){
			Log.i(TAG, "Video present in memory");
	        r = vf;
		} else if (vf.retrieve(context)){
			Log.i(TAG, "Retrieved Video from local storage.");
			r = vf;
		} else {
            Log.i(TAG, "Video not retrievable from local storage");
			r = null;
		}
		return r;
	}
	
	public GridElementFactory ensureGridElement(){
		gf = GridElementFactory.getFactoryInstance();
		GridElementFactory r;
		if (gf.hasInstances()){
			Log.i(TAG, "GridElement present in memory");
	        r = gf;
		} else if (gf.retrieve(context)){
			Log.i(TAG, "Retrieved GridElement from local storage.");
			r = gf;
		} else {
            Log.i(TAG, "GridElement not retrievable from local storage");
			r = null;
		}
		return r;
	}
	
	public FriendFactory retrieveFriend(){
		ff = FriendFactory.getFactoryInstance();
        ff.retrieve(context);
        return ff;
	}
	
	public UserFactory retrieveUser(){
		uf = UserFactory.getFactoryInstance();
        uf.retrieve(context);
        return uf;
	}
	
	public VideoFactory retrieveVideo(){
		vf = VideoFactory.getFactoryInstance();
		vf.retrieve(context);
		return vf;
	}
	
	public GridElementFactory retrieveGridElement(){
		gf = GridElementFactory.getFactoryInstance();
		gf.retrieve(context);
		return gf;
	}
	
	public void saveUser(){
		uf = UserFactory.getFactoryInstance();
		if (uf.hasInstances()){
			Log.i(TAG, "Saving User to local storage:" + UserFactory.current_user().attributes.toString());
			uf.save(context);
		} else {
			Log.i(TAG, "Not Saving User. No instances found");
		}
	}
	
	public void saveFriend(){
		ff = FriendFactory.getFactoryInstance();
		if (ff.hasInstances()){
			Log.i(TAG, "Saving Friend to local storage: " + ff.instances.size());
			ff.save(context);
		} else {
			Log.i(TAG, "Not Saving Friend. No instances found");
		}
	}
	
	public void saveVideo(){
		vf = VideoFactory.getFactoryInstance();
		if (vf.hasInstances()){
			Log.i(TAG, "Saving Video to local storage");
			vf.save(context);
		} else {
			Log.i(TAG, "Not saving Video. No instances found.");
		}
	}

	public void saveGridElement(){
		gf = GridElementFactory.getFactoryInstance();
		if (gf.hasInstances()){
			Log.i(TAG, "Saving Video to local storage");
			gf.save(context);
		} else {
			Log.i(TAG, "Not saving Video. No instances found.");
		}
	}

}
