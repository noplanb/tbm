package com.zazoapp.client.contactsgetter;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.zazoapp.client.contactsgetter.vectors.ContactVector;
import com.zazoapp.client.contactsgetter.vectors.EmailVector;
import com.zazoapp.client.contactsgetter.vectors.MobileVector;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by skamenkovych@codeminders.com on 9/16/2015.
 */
public class ContactInfo {
    private int id;
    private boolean hasPhoneNumber;
    private JSONContactInfo jsonContactInfo;

    private static final String ADDS_FAVORITE = "marked_as_favorite";

    public ContactInfo(int id, @NonNull String name) {
        this.id = id;
        hasPhoneNumber = false;
        jsonContactInfo = new JSONContactInfo(name);
        jsonContactInfo.addParam(ADDS_FAVORITE, false);
    }

    public void setFavorite(boolean favorite) {
        jsonContactInfo.addParam(ADDS_FAVORITE, favorite);
    }

    public boolean hasPhoneNumber() {
        return hasPhoneNumber;
    }

    public boolean addVector(ContactVector vec) {
        if (vec == null) {
            throw new NullPointerException("Vector can't be null");
        }
        if (TextUtils.isEmpty(vec.getName())) {
            throw new IllegalArgumentException("Vector name must be specified");
        }
        if (TextUtils.isEmpty(vec.getValue())) {
            throw new IllegalArgumentException("Vector value must be specified");
        }
        String normalizedValue = vec.normaliseValue();
        boolean result = !TextUtils.isEmpty(normalizedValue) && jsonContactInfo.addVector(vec);
        if (result && vec instanceof MobileVector) {
            hasPhoneNumber = true;
        }
        return result;
    }

    private static class JSONContactInfo {
        private String display_name;
        private LinkedTreeMap<String, Object> additions = new LinkedTreeMap<>();
        private Set<ContactVector> vectors = new HashSet<>();

        JSONContactInfo(String name) {
            display_name = name;
        }

        public void addParam(String paramKey, Object paramValue) {
            switch (paramKey) {
                case ADDS_FAVORITE:
                    break;
                default:
                    throw new IllegalArgumentException("paramKey for " + getClass().getSimpleName() + " isn't supported");
            }
            additions.put(paramKey, paramValue);
        }

        public boolean addVector(ContactVector vec) {
            if (!vectors.contains(vec)) {
                return vectors.add(vec);
            }
            return false;
        }
    }

    public JSONObject toJson() {
        Gson gson = new Gson();
        JSONObject object = null;
        try {
            object = new JSONObject(gson.toJson(jsonContactInfo));
        } catch (JSONException e) {
        }
        return object;
    }

    private static void main(String[] args) {
        //UserFactory.getFactoryInstance().makeInstance(null);
        ContactInfo local = new ContactInfo(1, "Sani");
        ContactVector vec2 = new EmailVector("test@test.com");
        vec2.addParam(EmailVector.ADDS_EMAIL_SENT, 11);
        local.addVector(vec2);
        vec2 = new EmailVector("test@test.com");
        local.addVector(vec2);
        vec2 = new EmailVector("test@test..com");
        local.addVector(vec2);
        vec2 = new MobileVector("+380-67.123..");
        local.addVector(vec2);
        vec2 = new MobileVector("+380-67.123..4567");
        local.addVector(vec2);
        vec2 = new MobileVector("931-668-1900");
        local.addVector(vec2);
        System.out.println(local.toJson());
        System.out.println("+380-67.123..".replaceAll("[^0-9+]", ""));
    }
}
