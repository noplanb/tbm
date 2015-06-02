package com.zazoapp.client.model;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;

public class ActiveModelsHandler implements UnexpectedTerminationHelper.TerminationCallback {
    public static final String USER_REGISTERED = "user_registered";
    private static final String TAG = ActiveModelsHandler.class.getSimpleName();

    private static ActiveModelsHandler instance;

    private UserFactory userFactory;
    private FriendFactory friendFactory;
    private IncomingVideoFactory incomingVideoFactory;
    private OutgoingVideoFactory outgoingVideoFactory;
    private GridElementFactory gridElementFactory;

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

    public void ensureAll() {
        userFactory = UserFactory.getFactoryInstance();
        friendFactory = FriendFactory.getFactoryInstance();
        incomingVideoFactory = IncomingVideoFactory.getFactoryInstance();
        gridElementFactory = GridElementFactory.getFactoryInstance();
        outgoingVideoFactory = OutgoingVideoFactory.getFactoryInstance();
        ensureUser();
        ensure(friendFactory);
        ensure(incomingVideoFactory);
        ensure(gridElementFactory);
        ensure(outgoingVideoFactory);
        migrateOldOutgoingVideoModel();
        new PreferencesHelper(context).putBoolean(USER_REGISTERED, User.isRegistered(context));
        Log.d(TAG, "ensureAll end");
    }

    public void saveAll() {
        save(userFactory);
        save(friendFactory);
        save(incomingVideoFactory);
        save(gridElementFactory);
        save(outgoingVideoFactory);
        new PreferencesHelper(context).putBoolean(USER_REGISTERED, User.isRegistered(context));
        Log.i(TAG, "saveAll end");
    }

    public void retrieveAll() {
        retrieve(UserFactory.getFactoryInstance());
        retrieve(FriendFactory.getFactoryInstance());
        retrieve(IncomingVideoFactory.getFactoryInstance());
        Log.i(TAG, "retrieveAll: retrieved " + IncomingVideoFactory.getFactoryInstance().count() + "videos");
        retrieve(GridElementFactory.getFactoryInstance());
        retrieve(OutgoingVideoFactory.getFactoryInstance());
    }

    public void destroyAll() {
        userFactory.destroyAll(context);
        friendFactory.destroyAll(context);
        incomingVideoFactory.destroyAll(context);
        gridElementFactory.destroyAll(context);
        outgoingVideoFactory.destroyAll(context);
    }

    public UserFactory ensureUser() {
        if (userFactory == null) {
            throw new NullPointerException("Probably you forgot to load data on application start");
        }
        if (ensure(userFactory) == null) {
            userFactory.makeInstance(context);
        }
        return userFactory;
    }

    public <K extends ActiveModel, T extends ActiveModelFactory<K>> T ensure(T factory) {
        final String model = factory.getModelClass().getSimpleName();
        if (factory.hasInstances()) {
            Log.i(TAG, String.format("%s present in memory", model));
        } else if (factory.retrieve(context)) {
            Log.i(TAG, String.format("Retrieved %s from local storage.", model));
        } else {
            Log.i(TAG, String.format("%s not retrievable from local storage", model));
            return null;
        }
        return factory;
    }

    public <K extends ActiveModel, T extends ActiveModelFactory<K>> T retrieve(T factory) {
        factory.retrieve(context);
        return factory;
    }

    public <K extends ActiveModel, T extends ActiveModelFactory<K>> void save(T factory) {
        final String model = factory.getModelClass().getSimpleName();
        if (factory.hasInstances()) {
            Log.i(TAG, String.format("Saving %s to local storage: %d", model, factory.count()));
            factory.save(context);
        } else {
            Log.i(TAG, String.format("Not Saving %s. No instances found", model));
        }
    }

    @Override
    public void onTerminate() {
        saveAll();
    }

    private void migrateOldOutgoingVideoModel() {
        if (friendFactory.getModelVersion() < 72) {
            for (Friend friend : friendFactory.all()) {
                String id = friend.getOutgoingVideoId();
                if (!TextUtils.isEmpty(id)) {
                    int status = friend.getOutgoingVideoStatus();
                    OutgoingVideo video = outgoingVideoFactory.makeInstance(context);
                    video.setVideoStatus(status);
                    video.set(Video.Attributes.ID, id);
                    video.set(Video.Attributes.FRIEND_ID, friend.getId());
                }
            }
            friendFactory.save(context);
            outgoingVideoFactory.save(context);
        }
    }
}
