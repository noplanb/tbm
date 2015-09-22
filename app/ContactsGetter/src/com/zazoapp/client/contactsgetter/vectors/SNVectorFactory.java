package com.zazoapp.client.contactsgetter.vectors;

import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;

/**
 * Created by skamenkovych@codeminders.com on 9/21/2015.
 */
public final class SNVectorFactory {

    private enum SNVectorType {
        SKYPE("skype", "com.skype.contacts.sync", ContactsContract.RawContacts.SOURCE_ID),
        LINKEDIN("linkedin", "com.linkedin.android", ContactsContract.RawContacts.SYNC2),
        WHATSAPP("whatsapp", "com.whatsapp", ContactsContract.RawContacts.SYNC1),
        KATE("vk", "com.perm.kate.account", ContactsContract.RawContacts.SOURCE_ID),
        VKONTAKTE("vk", "com.vkontakte.account", ContactsContract.RawContacts.SYNC1),
        GOOGLE_PLUS("google+", "com.google/plus", ContactsContract.RawContacts.SOURCE_ID), // account_type_and_data_set: com.google/plus | sourceid: g:107073129092125082676
        VIBER("viber", "com.viber.voip", ContactsContract.RawContacts.SYNC1), // account_type: com.viber.voip | sync1: 113.1752882236
        TELEGRAM("telegram", "org.telegram.messenger", ContactsContract.RawContacts.SYNC1),
        ;

        private String vectorName;
        private String account;
        private String dataId;

        SNVectorType(String name, String acc, String id) {
            vectorName = name;
            account = acc;
            dataId = id;
        }
    }

    public static ContactVector produce(@NonNull Cursor c) {
        int accId = c.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE);
        int dataSetId = c.getColumnIndex(ContactsContract.RawContacts.DATA_SET);
        if (accId < 0 || dataSetId < 0) {
            return null;
        }
        String dataSet = c.getString(dataSetId);
        String acc = c.getString(accId);
        if (acc != null) {
            if (dataSet != null) {
                acc = acc.concat("/").concat(dataSet);
            }
            for (SNVectorType vg : SNVectorType.values()) {
                if (vg.account.equals(acc)) {
                    int index = c.getColumnIndex(vg.dataId);
                    if (index < 0) {
                        return null;
                    }
                    return SocialNetworkVector.getForType(vg, c.getString(index));
                }
            }
        }
        return null;
    }

    private static class SocialNetworkVector extends ContactVector {

        private final transient SNVectorType vectorType;

        private SocialNetworkVector(SNVectorType type, String value) {
            super(type.vectorName, value);
            vectorType = type;
        }

        @Override
        public String normaliseValue() {
            switch (vectorType) {
                case GOOGLE_PLUS:
                    value = value.replaceAll("[^0-9]", "");
                    break;
                case VIBER:
                    int dotIndex = value.indexOf('.');
                    value = (dotIndex <= 0 || dotIndex == value.length() - 1) ? null : value.substring(dotIndex + 1, value.length()).replaceAll("[^0-9]", "");
            }
            return value;
        }

        public static ContactVector getForType(SNVectorType type, String value) {
            if (value == null) {
                return null;
            }
            return new SocialNetworkVector(type, value);
        }
    }

    private static void main(String[] args) {
        // Viber normalization check
        String[] arr = new String[]{"", ".", "1.", ".2", "1.2"};
        for (String value : arr) {
            System.out.println(new SocialNetworkVector(SNVectorType.VIBER, value).normaliseValue());
        }
    }
}
