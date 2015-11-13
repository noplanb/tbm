package com.zazoapp.client.ui.dialogs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import cz.msebera.android.httpclient.protocol.HTTP;

/**
 * Created by skamenkovych@codeminders.com on 7/15/2015.
 */
public enum InviteIntent {
    SMS {
        /**
         * Returns an intent to send SMS.<br>
         * Pass data with {@link InviteIntent#PHONE_NUMBER_KEY} with recipient's phone number
         * and {@link InviteIntent#MESSAGE_KEY} with message body
         */
        @Override
        public Intent getIntent(Bundle bundle) {
            Intent i = new Intent(Intent.ACTION_SENDTO);
            Uri smsUri = Uri.parse("smsto:" + bundle.getString(PHONE_NUMBER_KEY));
            i.putExtra("sms_body", bundle.getString(MESSAGE_KEY));
            i.setData(smsUri);
            return i;
        }
    },
    EMAIL {
        /**
         * Returns an intent to send an Email.<br>
         * Pass data with {@link InviteIntent#EMAIL_KEY} with recipient's email address,
         * {@link InviteIntent#SUBJECT_KEY} with email subject
         * and {@link InviteIntent#MESSAGE_KEY} with message body
         */
        @Override
        public Intent getIntent(Bundle bundle) {
            Intent i = new Intent(Intent.ACTION_SENDTO);
            String email = "mailto:" + bundle.getString(EMAIL_KEY)
                    + "?subject=" + Uri.encode(bundle.getString(SUBJECT_KEY))
                    + "&body=" + Uri.encode(bundle.getString(MESSAGE_KEY));
            i.setData(Uri.parse(email));
            return i;
        }
    },
    TEXT {
        /**
         * Returns an intent to send plain text.<br>
         * Pass data with {@link InviteIntent#EMAIL_KEY} with recipient's email address,
         * {@link InviteIntent#SUBJECT_KEY} with email subject
         * and {@link InviteIntent#MESSAGE_KEY} with message body
         */
        @Override
        public Intent getIntent(Bundle bundle) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(HTTP.PLAIN_TEXT_TYPE);
            i.putExtra(Intent.EXTRA_TEXT, bundle.getString(MESSAGE_KEY));
            i.putExtra(Intent.EXTRA_SUBJECT, bundle.getString(SUBJECT_KEY));
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{bundle.getString(EMAIL_KEY)});
            return i;
        }
    };

    public static final String PHONE_NUMBER_KEY = "phone_number_key";
    public static final String MESSAGE_KEY = "message_key";
    public static final String EMAIL_KEY = "email_key";
    public static final String SUBJECT_KEY = "subject_key";
    public static final int INVITATION_REQUEST_ID = 101;

    public abstract Intent getIntent(Bundle bundle);
}
