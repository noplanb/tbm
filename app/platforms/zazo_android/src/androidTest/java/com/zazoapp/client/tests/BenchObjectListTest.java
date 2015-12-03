package com.zazoapp.client.tests;

import android.test.InstrumentationTestCase;
import com.zazoapp.client.bench.BenchObject;
import com.zazoapp.client.bench.BenchObjectList;
import com.zazoapp.client.bench.ContactsGroup;
import com.zazoapp.client.bench.GeneralContactsGroup;
import junit.framework.Assert;
import org.junit.Before;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for BenchObjectList
 * Created by Serhii on 27.11.2015.
 */
public class BenchObjectListTest extends InstrumentationTestCase {

    @Before
    public void setUp() {
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
    }

    public void testAdding() {
        BenchObjectList list = new BenchObjectList();
        BenchObject zazoFriend = getBenchObjectWithGroup(GeneralContactsGroup.ZAZO_FRIEND);
        BenchObject smsContact = getBenchObjectWithGroup(GeneralContactsGroup.SMS_CONTACTS);
        BenchObject contact = getBenchObjectWithGroup(GeneralContactsGroup.CONTACTS);

        list.add(zazoFriend);
        Assert.assertTrue(list.isFirstForGroup(0, ContactsGroup.getGeneralGroup(GeneralContactsGroup.ZAZO_FRIEND)));
        Assert.assertTrue(list.isLastForGroup(0, ContactsGroup.getGeneralGroup(GeneralContactsGroup.ZAZO_FRIEND)));
        Assert.assertFalse(list.isFirstForGroup(0, ContactsGroup.getGeneralGroup(GeneralContactsGroup.SMS_CONTACTS)));
        Assert.assertFalse(list.isLastForGroup(0, ContactsGroup.getGeneralGroup(GeneralContactsGroup.SMS_CONTACTS)));
        list.add(smsContact);
        Assert.assertEquals(GeneralContactsGroup.ZAZO_FRIEND, list.getGroup(0).getGeneralGroup());
        Assert.assertEquals(GeneralContactsGroup.SMS_CONTACTS, list.getGroup(1).getGeneralGroup());
        Assert.assertTrue(list.isFirstForGroup(0, ContactsGroup.getGeneralGroup(GeneralContactsGroup.ZAZO_FRIEND)));
        Assert.assertTrue(list.isLastForGroup(0, ContactsGroup.getGeneralGroup(GeneralContactsGroup.ZAZO_FRIEND)));
        Assert.assertTrue(list.isFirstForGroup(1, ContactsGroup.getGeneralGroup(GeneralContactsGroup.SMS_CONTACTS)));
        Assert.assertTrue(list.isLastForGroup(1, ContactsGroup.getGeneralGroup(GeneralContactsGroup.SMS_CONTACTS)));
        list.add(contact);
        list.add(contact);
        Assert.assertEquals(3, list.getGroupCount());
    }

    public void testAddingWrongGroup() {
        BenchObjectList list = new BenchObjectList();
        BenchObject zazoFriend = getBenchObjectWithGroup(GeneralContactsGroup.ZAZO_FRIEND);
        BenchObject smsContact = getBenchObjectWithGroup(GeneralContactsGroup.SMS_CONTACTS);
        list.add(zazoFriend);
        list.add(smsContact);
        try {
            list.add(zazoFriend);
        } catch (Exception e) {
            return;
        }
        Assert.fail("Duplicated group was added");
    }

    public void testAddingGroups() {
        BenchObjectList list = new BenchObjectList();
        BenchObject zazoFriend = getBenchObjectWithGroup(GeneralContactsGroup.ZAZO_FRIEND);
        BenchObject smsContact = getBenchObjectWithGroup(GeneralContactsGroup.SMS_CONTACTS);
        BenchObject contact = getBenchObjectWithGroup(GeneralContactsGroup.CONTACTS);
        BenchObject[] objects = {zazoFriend, smsContact, contact};
        final int COUNT = 20;
        for (BenchObject benchObject : objects) {
            ArrayList<BenchObject> groupList = new ArrayList<>(COUNT);
            for (int i = 0; i < COUNT; i++) {
                groupList.add(benchObject);
            }
            list.addGroup(groupList, benchObject.getGroup());
            if (benchObject.equals(zazoFriend)) {
                Assert.assertEquals(GeneralContactsGroup.ZAZO_FRIEND, list.getGroup(1).getGeneralGroup());
            }
        }
        Assert.assertTrue(list.isFirstForGroup(0, ContactsGroup.getGeneralGroup(GeneralContactsGroup.ZAZO_FRIEND)));
        Assert.assertTrue(list.isFirstForGroup(COUNT, ContactsGroup.getGeneralGroup(GeneralContactsGroup.SMS_CONTACTS)));
        Assert.assertTrue(list.isFirstForGroup(2*COUNT, ContactsGroup.getGeneralGroup(GeneralContactsGroup.CONTACTS)));
        Assert.assertTrue(list.isLastForGroup(COUNT - 1, ContactsGroup.getGeneralGroup(GeneralContactsGroup.ZAZO_FRIEND)));
        Assert.assertTrue(list.isLastForGroup(2 * COUNT - 1, ContactsGroup.getGeneralGroup(GeneralContactsGroup.SMS_CONTACTS)));
        Assert.assertTrue(list.isLastForGroup(3 * COUNT - 1, ContactsGroup.getGeneralGroup(GeneralContactsGroup.CONTACTS)));
        for (int i = 0; i < list.size(); i++) {
            GeneralContactsGroup group = (i >= 2*COUNT) ? GeneralContactsGroup.CONTACTS : (i < COUNT) ? GeneralContactsGroup.ZAZO_FRIEND : GeneralContactsGroup.SMS_CONTACTS;
            Assert.assertEquals(group, list.getGroup(i).getGeneralGroup());
        }
    }

    private BenchObject getBenchObjectWithGroup(GeneralContactsGroup group) {
        BenchObject object = mock(BenchObject.class);
        when(object.getGroup()).thenReturn(ContactsGroup.getGeneralGroup(group));
        return object;
    }
}
