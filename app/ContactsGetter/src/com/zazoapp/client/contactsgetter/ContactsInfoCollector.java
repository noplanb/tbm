package com.zazoapp.client.contactsgetter;

import com.google.gson.internal.LinkedTreeMap;

/**
 * Created by skamenkovych@codeminders.com on 9/16/2015.
 */
public class ContactsInfoCollector {
    public interface ContactsInfoCollectedCallback {
        void onInfoCollected(LinkedTreeMap<Integer, ContactInfo> contacts);
    }
}
