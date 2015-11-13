package com.zazoapp.client.model;

import com.zazoapp.client.multimedia.VideoIdUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 5/29/2015.
 */
public abstract class Video extends ActiveModel {

    public static class VideoTimestampComparator <T extends Video> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            return VideoIdUtils.timeStampFromVideoId(o1.getId()).compareTo(VideoIdUtils.timeStampFromVideoId(o2.getId()));
        }
    }

    public static class VideoTimestampReverseComparator <T extends Video> implements Comparator<T> {
        @Override
        public int compare(T o1, T o2) {
            return VideoIdUtils.timeStampFromVideoId(o2.getId()).compareTo(VideoIdUtils.timeStampFromVideoId(o1.getId()));
        }
    }

    public static class Attributes{
        public static final String ID  = "id";
        public static final String FRIEND_ID = "friendId";
        public static final String STATUS = "status";
        public static final String RETRY_COUNT = "downloadRetryCount"; // keep legacy value
    }

    @Override
    public List<String> attributeList() {
        final String[] a = {
                Attributes.ID,
                Attributes.FRIEND_ID,
                Attributes.STATUS,
                Attributes.RETRY_COUNT
        };
        return new ArrayList<>(Arrays.asList(a));
    }

    // Video status
    public void setVideoStatus(int status){
        set(Attributes.STATUS, ((Integer) status).toString());
    }

    public int getVideoStatus(){
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

}
