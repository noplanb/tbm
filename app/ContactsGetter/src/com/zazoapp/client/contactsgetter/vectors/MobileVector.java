package com.zazoapp.client.contactsgetter.vectors;

import com.zazoapp.client.contactsgetter.ContactsManager;

/**
 * Created by skamenkovych@codeminders.com on 9/17/2015.
 */
public class MobileVector extends ContactVector {

    public static final String ADDS_SMS_SENT = "sms_messages_sent";
    private static final String VEC_MOBILE = "mobile";

    public MobileVector(String value) {
        super(VEC_MOBILE, value);
    }

    @Override
    public void addParam(String paramKey, Object paramValue) {
        switch (paramKey) {
            case ADDS_SMS_SENT:
                break;
            default:
                throw new IllegalArgumentException("paramKey for " + getClass().getSimpleName() + " isn't supported");
        }
        super.addParam(paramKey, paramValue);
    }

    @Override
    public String normaliseValue() {
        if (value != null) {
            value = value.replaceAll("[^0-9+]", "");
        }
        value = ContactsManager.getValidE164ForNumber(value);
        return value;
    }
}
