package com.zazoapp.client.bench;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * An interface of contacts group
 * Created by skamenkovych@codeminders.com on 12/1/2015.
 */
public class ContactsGroup {
    private CharSequence text;
    private int iconId;
    private Object parentGroup;
    private GeneralContactsGroup generalGroup;
    private boolean hasSubGroups;

    private static final Map<GeneralContactsGroup, ContactsGroup> GENERAL_GROUPS = new HashMap<>();
    static {
        for (GeneralContactsGroup generalContactsGroup : GeneralContactsGroup.values()) {
            getGeneralGroup(generalContactsGroup, generalContactsGroup == GeneralContactsGroup.CONTACTS);
        }
    }
    private ContactsGroup() {
    }

    private static ContactsGroup getGeneralGroup(GeneralContactsGroup generalGroup, boolean hasSubGroups) {
        ContactsGroup group = new ContactsGroup();
        group.parentGroup = generalGroup;
        group.generalGroup = generalGroup;
        group.hasSubGroups = hasSubGroups;
        GENERAL_GROUPS.put(generalGroup, group);
        return group;
    }

    public static ContactsGroup getGeneralGroup(GeneralContactsGroup generalGroup) {
        return GENERAL_GROUPS.get(generalGroup);
    }

    public static ContactsGroup getSubGroup(ContactsGroup parentGroup, CharSequence text) {
        if (!parentGroup.hasSubGroups) {
            throw new IllegalArgumentException("Parent group must be able to contain subGroups");
        }
        ContactsGroup group = new ContactsGroup();
        group.parentGroup = parentGroup;
        group.generalGroup = parentGroup.getGeneralGroup();
        group.text = text;
        return group;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public int getIcon() {
        return hasSubGroups ? 0 : generalGroup.getIconId();
    }

    public boolean hasSubGroups() {
        return hasSubGroups;
    }

    public boolean isGeneralGroup() {
        return generalGroup.equals(parentGroup);
    }

    public GeneralContactsGroup getGeneralGroup() {
        ContactsGroup group = this;
        while (group.parentGroup instanceof ContactsGroup) {
            group = (ContactsGroup) group.parentGroup;
        }
        return (GeneralContactsGroup) group.parentGroup;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(generalGroup);
        builder.append(parentGroup);
        builder.append(hasSubGroups);
        builder.append(text);
        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ContactsGroup) {
            EqualsBuilder builder = new EqualsBuilder();
            builder.append(generalGroup, ((ContactsGroup) o).generalGroup);
            builder.append(parentGroup, ((ContactsGroup) o).parentGroup);
            builder.append(hasSubGroups, ((ContactsGroup) o).hasSubGroups);
            builder.append(text, ((ContactsGroup) o).text);
            return builder.isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (!isGeneralGroup()) {
            builder.append(text).append(" ");
        }
        builder.append(generalGroup);
        return builder.toString();
    }
}
