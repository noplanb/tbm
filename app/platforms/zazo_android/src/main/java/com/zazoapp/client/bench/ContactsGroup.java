package com.zazoapp.client.bench;

import com.zazoapp.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 11/23/2015.
 */
public enum ContactsGroup {
    ALL(R.drawable.ic_contacts_black, R.string.contacts_groups_all),
    ZAZO_FRIEND(R.drawable.ic_zazo_letter, R.string.contacts_groups_zazo_friends),
    ZAZO_GROUPS(R.drawable.ic_contacts_black, R.string.contacts_groups_zazo_groups, false),
    FAVORITES(R.drawable.ic_contacts_black, R.string.contacts_groups_favorites, false),
    SMS_CONTACTS(R.drawable.ic_contacts_black, R.string.contacts_groups_sms_contacts),
    CONTACTS(R.drawable.ic_contacts_black, R.string.contacts_groups_contacts);

    private int iconId;

    private int titleId;
    private boolean enabled = true;

    ContactsGroup(int iconId, int titleId) {
        this.iconId = iconId;
        this.titleId = titleId;
    }

    ContactsGroup(int iconId, int titleId, boolean enabled) {
        this(iconId, titleId);
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTitleId() {
        return titleId;
    }

    public int getIconId() {
        return iconId;
    }

    /**
     *
     * @return list of all enabled groups except {@link ContactsGroup#ALL}
     */
    public static List<ContactsGroup> getActive() {
        ArrayList<ContactsGroup> activeGroups = new ArrayList<>();
        for (ContactsGroup group : values()) {
            if (group.enabled && group != ALL) {
                activeGroups.add(group);
            }
        }
        return activeGroups;
    }

}
