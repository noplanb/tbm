package com.zazoapp.client.bench;

import com.zazoapp.client.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 11/23/2015.
 */
public enum GeneralContactsGroup {
    ALL(R.drawable.ic_contacts_group_all, R.string.contacts_groups_all),
    ZAZO_FRIEND(R.drawable.ic_contacts_group_zazo_friends, R.string.contacts_groups_zazo_friends),
    ZAZO_GROUPS(R.drawable.ic_contacts_group_zazo_groups, R.string.contacts_groups_zazo_groups, false),
    FAVORITES(R.drawable.ic_contacts_group_favorites, R.string.contacts_groups_favorites),
    SMS_CONTACTS(R.drawable.ic_contacts_group_sms, R.string.contacts_groups_sms_contacts),
    CONTACTS(R.drawable.ic_contacts_group_contacts, R.string.contacts_groups_contacts);

    private int iconId;

    private int titleId;
    private boolean enabled = true;

    GeneralContactsGroup(int iconId, int titleId) {
        this.iconId = iconId;
        this.titleId = titleId;
    }

    GeneralContactsGroup(int iconId, int titleId, boolean enabled) {
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
     * @return list of all enabled groups
     */
    public static List<GeneralContactsGroup> getActive() {
        ArrayList<GeneralContactsGroup> activeGroups = new ArrayList<>();
        for (GeneralContactsGroup group : values()) {
            if (group.enabled) {
                activeGroups.add(group);
            }
        }
        return activeGroups;
    }

}
