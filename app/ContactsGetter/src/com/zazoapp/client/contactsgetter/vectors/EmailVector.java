package com.zazoapp.client.contactsgetter.vectors;

/**
 * Created by skamenkovych@codeminders.com on 9/17/2015.
 */
public class EmailVector extends ContactVector {

    public static final String ADDS_EMAIL_SENT = "email_messages_sent";
    private static final String VEC_EMAIL = "email";

    public EmailVector(String value) {
        super(VEC_EMAIL, value);
    }

    @Override
    public void addParam(String paramKey, Object paramValue) {
        switch (paramKey) {
            case ADDS_EMAIL_SENT:
                break;
            default:
                throw new IllegalArgumentException("paramKey for " + getClass().getSimpleName() + " isn't supported");
        }
        super.addParam(paramKey, paramValue);
    }

    @Override
    public String normaliseValue() {
        if (value != null) {
            // Pattern from answer http://stackoverflow.com/a/16058059/2894819
            // Just literal check to filter wrong entered addresses
            String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
            java.util.regex.Matcher m = p.matcher(value);
            if (!m.matches()) {
                value = null;
            }
        }
        return value;
    }
}
