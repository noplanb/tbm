package com.zazoapp.client.model;

import android.content.Context;

public class UserFactory extends ActiveModelFactory<User> {

    public static class ServerParamKeys {
        public static final String FIRST_NAME = "first_name";
        public static final String LAST_NAME = "last_name";
        public static final String MOBILE_NUMBER = "mobile_number";
        public static final String ID = "id";
        public static final String MKEY = "mkey";
        public static final String AUTH = "auth";
        public static final String DEVICE_PLATFORM = "device_platform";
        public static final String VERIFICATION_VIA = "via";
        public static final String VERIFICATION_CODE = "verification_code";
        public static final String VERIFICATION_FORCE_SMS = "force_sms";
        public static final String VERIFICATION_FORCE_CALL = "force_call";
    }

    public static class VerificationCodeVia {
        public static final String CALL = "call";
        public static final String SMS = "sms";
    }

    private static UserFactory instance = null;

    public static UserFactory getFactoryInstance() {
        if (instance == null)
            instance = new UserFactory();
        return instance;
    }

    @Override
    public User makeInstance(Context context) {
        if (instances.isEmpty()) {
            return super.makeInstance(context);
        } else {
            return instances.get(0);
        }
    }

    @Override
    public Class<User> getModelClass() {
        return User.class;
    }

    public static User current_user() {
        UserFactory uf = UserFactory.getFactoryInstance();
        if (uf.hasInstances()) {
            return uf.instances.get(0);
        } else {
            return null;
        }
    }
}
