package com.zazoapp.client.notification;

import android.os.Parcel;
import android.os.Parcelable;
import com.zazoapp.client.features.friendfinder.Suggestion;

import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 4/28/2016.
 */
public class NotificationSuggestion extends Suggestion implements Parcelable {
    private String nkey;

    public NotificationSuggestion(String name, String nkey, List<String> phones) {
        super(name, phones);
        this.nkey = nkey;
    }

    protected NotificationSuggestion(Parcel in) {
        super(in.readString(), in.readArrayList(null));
        nkey = in.readString();
    }

    public static final Creator<NotificationSuggestion> CREATOR = new Creator<NotificationSuggestion>() {
        @Override
        public NotificationSuggestion createFromParcel(Parcel in) {
            return new NotificationSuggestion(in);
        }

        @Override
        public NotificationSuggestion[] newArray(int size) {
            return new NotificationSuggestion[size];
        }
    };

    public String getNkey() {
        return nkey;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getName());
        dest.writeList(getPhones());
        dest.writeString(nkey);
    }
}
