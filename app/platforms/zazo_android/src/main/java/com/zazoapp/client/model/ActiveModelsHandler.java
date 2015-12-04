package com.zazoapp.client.model;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActiveModelsHandler implements UnexpectedTerminationHelper.TerminationCallback, ActiveModelFactory.ModelChangeCallback {
    public static final String USER_REGISTERED = "user_registered";
    private static final String TAG = ActiveModelsHandler.class.getSimpleName();
    public static final String MODEL_VERSION_PREF = "model_version_pref";
    public static final int MODEL_VERSION = 5;

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
        removeCallbacks();
        boolean upgraded = onUpgrade(new PreferencesHelper(context).getInt(MODEL_VERSION_PREF, 1), MODEL_VERSION);
        if (!upgraded) {
            ensureUser();
            ensure(friendFactory);
            ensure(incomingVideoFactory);
            ensure(gridElementFactory);
            ensure(outgoingVideoFactory);
            new PreferencesHelper(context).putBoolean(USER_REGISTERED, User.isRegistered(context));
            Log.d(TAG, "ensureAll end");
        }
        setCallbacks();
    }

    public void saveAll() {
        for (ActiveModelFactory<?> factory : getFactories()) {
            save(factory);
        }
        PreferencesHelper prefs = new PreferencesHelper(context);
        prefs.putBoolean(USER_REGISTERED, User.isRegistered(context));
        prefs.putInt(MODEL_VERSION_PREF, MODEL_VERSION);
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
        for (ActiveModelFactory<?> factory : getFactories()) {
            factory.destroyAll(context);
        }
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

    private boolean onUpgrade(int currentVersion, int newVersion) {
        if (currentVersion >= newVersion) {
            return false;
        }
        switch (currentVersion) {
            case 1:
                ModelUpgradeHelper.upgradeTo2(this, context);
            case 2:
                ModelUpgradeHelper.upgradeTo3(this, context);
            case 3:
                ModelUpgradeHelper.upgradeTo4(this, context);
            case 4:
                ModelUpgradeHelper.upgradeTo5(this, context);
        }
        saveAll();
        return true;
    }

    public ActiveModelFactory<?> getModelFromIntent(Intent intent) {
        int modelId = intent.getIntExtra(IntentHandlerService.IntentParamKeys.MODEL, -1);
        if (modelId == -1) {
            return null;
        }
        switch (Model.values()[modelId]) {
            case USER: return userFactory;
            case FRIEND: return friendFactory;
            case GRID_ELEMENT: return gridElementFactory;
            case INCOMING_VIDEO: return incomingVideoFactory;
            case OUTGOING_VIDEO: return outgoingVideoFactory;
            default: return null;
        }
    }

    private void removeCallbacks() {
        for (ActiveModelFactory<?> factory : getFactories()) {
            factory.removeCallback(this);
        }
    }

    private void setCallbacks() {
        for (ActiveModelFactory<?> factory : getFactories()) {
            factory.addCallback(this);
        }
    }

    @Override
    public void onModelChanged(ActiveModelFactory<?> factory, ActiveModelFactory.ModelChangeType changeType) {
        if (!TbmApplication.getInstance().isForeground()) {
            Intent intent = new Intent(context, IntentHandlerService.class);
            intent.setAction(IntentHandlerService.IntentActions.SAVE_MODEL);
            intent.putExtra(IntentHandlerService.IntentParamKeys.MODEL, Model.getId(factory.getModelClass()));
            context.startService(intent);
        }
    }

    private List<ActiveModelFactory<?>> getFactories() {
        ArrayList<ActiveModelFactory<?>> factories = new ArrayList<>();
        Collections.addAll(factories, userFactory, friendFactory, incomingVideoFactory, gridElementFactory, outgoingVideoFactory);
        return factories;
    }

    public enum Model {
        USER(User.class),
        FRIEND(Friend.class),
        INCOMING_VIDEO(IncomingVideo.class),
        OUTGOING_VIDEO(OutgoingVideo.class),
        GRID_ELEMENT(GridElement.class);

        private final Class<? extends ActiveModel> clazz;

        Model(Class<? extends ActiveModel> userClass) {
            clazz = userClass;
        }

        public static int getId(Class<? extends ActiveModel> clazz) {
            for (Model model : values()) {
                if (model.clazz.equals(clazz)) {
                    return model.ordinal();
                }
            }
            return -1;
        }
    }

    public static Model getModel(ActiveModelFactory<?> factory) {
        int id = Model.getId(factory.getModelClass());
        if (id == -1) {
            return null;
        } else {
            return Model.values()[id];
        }
    }
}