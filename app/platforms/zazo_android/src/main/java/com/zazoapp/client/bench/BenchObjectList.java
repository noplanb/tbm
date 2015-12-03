package com.zazoapp.client.bench;

import android.util.SparseArray;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 11/26/2015.
 */
public final class BenchObjectList extends ArrayList<BenchObject> {
    private SparseArray<Group> startEndPoints = new SparseArray<>();
    private LinkedHashSet<Group> groups = new LinkedHashSet<>();
    private Group lastGroup;

    private static class Group {
        ContactsGroup type;
        int start;
        int end;

        Group(ContactsGroup type, int start, int end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Group && type.equals(((Group) o).type);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }
    }

    @Override
    public boolean add(BenchObject object) {
        int currentIndex = size();
        boolean result = super.add(object);
        if (result) {
            ContactsGroup objectGroup = object.getGroup();
            if (currentIndex == 0) {
                Group group = new Group(objectGroup, 0, 0);
                groups.add(group);
                startEndPoints.put(0, group);
                lastGroup = group;
            } else {
                boolean groupThreshold = !lastGroup.type.equals(objectGroup);
                if (groupThreshold) {
                    Group group = new Group(objectGroup, currentIndex, currentIndex);
                    if (groups.contains(group)) {
                        super.remove(currentIndex);
                        throw new IllegalArgumentException("Group of objects can't be added twice");
                    }
                    groups.add(group);
                    startEndPoints.put(currentIndex, group);
                    startEndPoints.put(currentIndex - 1, lastGroup);
                    lastGroup.end = currentIndex - 1;
                    lastGroup = group;
                } else {
                    lastGroup.end = currentIndex;
                }
            }
        }
        return result;
    }

    @Override
    public void add(int index, BenchObject object) {
        //super.add(index, object);
        throw new NotImplementedException("");
    }

    @Override
    public boolean addAll(Collection<? extends BenchObject> collection) {
        //return super.addAll(collection);
        throw new UnsupportedOperationException("Use addGroup instead");
    }

    @Override
    public boolean addAll(int index, Collection<? extends BenchObject> collection) {
        //return super.addAll(index, collection);
        throw new UnsupportedOperationException("Use addGroup instead");
    }

    public boolean addGroup(Collection<? extends BenchObject> collection, ContactsGroup groupType) {
        int currentIndex = size();
        boolean result = super.addAll(collection);
        if (result) {
            if (currentIndex == 0) {
                Group group = new Group(groupType, 0, size() - 1);
                groups.add(group);
                startEndPoints.put(0, group);
                lastGroup = group;
            } else {
                boolean groupThreshold = !lastGroup.type.equals(groupType);
                if (groupThreshold) {
                    Group group = new Group(groupType, currentIndex, size() - 1);
                    if (groups.contains(group)) {
                        super.removeRange(currentIndex, size());
                        throw new IllegalArgumentException("Group of objects can't be added twice");
                    }
                    groups.add(group);
                    startEndPoints.put(currentIndex, group);
                    startEndPoints.put(currentIndex - 1, lastGroup);
                    lastGroup.end = currentIndex - 1;
                    lastGroup = group;
                } else {
                    lastGroup.end = size() - 1;
                }
            }
        }
        return result;
    }

    public boolean addWithSubgroups(BenchObjectList contactBenchObjects) {
        int currentIndex = size();
        boolean result = super.addAll(contactBenchObjects);
        if (result) {
            if (currentIndex == 0) {
                groups = (LinkedHashSet<Group>) contactBenchObjects.groups.clone();
                startEndPoints = contactBenchObjects.startEndPoints.clone();
            } else {
                for (Group group : contactBenchObjects.groups) {
                    if (groups.contains(group)) {
                        super.removeRange(currentIndex, size());
                        throw new IllegalArgumentException("Group of objects can't be added twice");
                    }
                    Group newGroup = new Group(group.type, currentIndex + group.start, currentIndex + group.end);
                    groups.add(newGroup);
                    startEndPoints.put(newGroup.start, newGroup);
                    startEndPoints.put(newGroup.end, newGroup);
                }
                lastGroup.end = currentIndex - 1;
                startEndPoints.put(lastGroup.end, lastGroup);
            }
            Group oldLast = contactBenchObjects.lastGroup;
            lastGroup = new Group(oldLast.type, currentIndex + oldLast.start, currentIndex + oldLast.end);
        }
        return result;
    }

    public boolean isFirstForGroup(int position, ContactsGroup groupType) {
        Group group = startEndPoints.get(position);
        return group != null && group.start == position && groupType.equals(group.type);
    }

    public boolean isLastForGroup(int position, ContactsGroup groupType) {
        Group group = (position == size() - 1) ? lastGroup : startEndPoints.get(position);
        return group != null && group.end == position && groupType.equals(group.type);
    }

    public ContactsGroup getGroup(int position) {
        for (Group group : groups) {
            if (position >= group.start && position <= group.end) {
                return group.type;
            }
        }
        return null;
    }

    public int getGroupCount() {
        return groups.size();
    }

    public List<ContactsGroup> getGroups() {
        ArrayList<ContactsGroup> groupList = new ArrayList<>(groups.size());
        for (Group group : groups) {
            groupList.add(group.type);
        }
        return groupList;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        //super.removeRange(fromIndex, toIndex);
        throw new UnsupportedOperationException("Don't remove objects from this BenchObjectList");
    }

    @Override
    public boolean remove(Object object) {
        //return super.remove(object);
        throw new UnsupportedOperationException("Don't remove objects from this BenchObjectList");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        //return super.removeAll(collection);
        throw new UnsupportedOperationException("Don't remove objects from this BenchObjectList");
    }

    @Override
    public BenchObject remove(int index) {
        //return super.remove(index);
        throw new UnsupportedOperationException("Don't remove objects from this BenchObjectList");
    }

    @Override
    public void clear() {
        super.clear();
        startEndPoints.clear();
        groups.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        BenchObjectList list = (BenchObjectList) super.clone();
        list.groups = (LinkedHashSet<Group>) this.groups.clone();
        list.startEndPoints = this.startEndPoints.clone();
        list.lastGroup = this.lastGroup;
        return list;
    }
}
