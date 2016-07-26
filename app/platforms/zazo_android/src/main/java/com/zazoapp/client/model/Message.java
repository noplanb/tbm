package com.zazoapp.client.model;

import android.content.Context;
import com.zazoapp.client.core.MessageType;
import com.zazoapp.client.multimedia.VideoIdUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 5/29/2015.
 */
public abstract class Message extends ActiveModel {

    public static class TimestampComparator<T extends Message> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            return VideoIdUtils.timeStampFromVideoId(o1.getId()).compareTo(VideoIdUtils.timeStampFromVideoId(o2.getId()));
        }
    }

    public static class TimestampReverseComparator<T extends Message> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            return VideoIdUtils.timeStampFromVideoId(o2.getId()).compareTo(VideoIdUtils.timeStampFromVideoId(o1.getId()));
        }
    }

    public static class Attributes {
        public static final String ID  = "id";
        public static final String FRIEND_ID = "friendId";
        public static final String STATUS = "status";
        public static final String RETRY_COUNT = "downloadRetryCount"; // keep legacy value
        public static final String TYPE = "type";
    }

    @Override
    public List<String> attributeList() {
        final String[] a = {
                Attributes.ID,
                Attributes.FRIEND_ID,
                Attributes.STATUS,
                Attributes.RETRY_COUNT,
                Attributes.TYPE
        };
        return new ArrayList<>(Arrays.asList(a));
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        setType(MessageType.VIDEO); // default value, should be reset after creation
    }

    @Override
    public boolean validate() {
        return notEmpty(getId());
    }

    // Video status
    public void setStatus(int status){
        set(Attributes.STATUS, ((Integer) status).toString());
    }

    public int getStatus(){
        return Integer.parseInt(get(Attributes.STATUS));
    }

    // Retry count
    public void setRetryCount(int retryCount){
        set(Attributes.RETRY_COUNT, ((Integer) retryCount).toString());
    }

    public int getRetryCount(){
        return Integer.parseInt(get(Attributes.RETRY_COUNT));
    }

    public String getFriendId() {
        return get(Attributes.FRIEND_ID);
    }

    public String getType() {
        return get(Attributes.TYPE);
    }

    public void setType(String type) {
        set(Attributes.TYPE, type);
    }

    public void setType(MessageType type) {
        set(Attributes.TYPE, type.getName());
    }
}
