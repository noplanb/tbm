package com.zazoapp.client.model;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.RemoteStorageHandler;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.multimedia.ThumbnailRetriever;
import com.zazoapp.client.network.FileDeleteService;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.network.FileUploadService;
import com.zazoapp.client.notification.NotificationHandler;
import com.zazoapp.client.utilities.Convenience;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Friend extends ActiveModel{

    private static final String MP4 = ".mp4";
    private static final String PNG = ".png";

    public interface VideoStatusChangedCallback {
        void onVideoStatusChanged(Friend friend);
    }

    /**
     * Normal state machine (one message): NEW -> QUEUED -> UPLOADING -> UPLOADED -> DOWNLOADED -(onViewed)-> VIEWED
     */
    public static class OutgoingVideoStatus{
		public static final int NONE = 0;
		public static final int NEW = 1;
		public static final int QUEUED = 2;
		public static final int UPLOADING = 3;
		public static final int UPLOADED = 4;
		public static final int DOWNLOADED = 5;
		public static final int VIEWED = 6;
		public static final int FAILED_PERMANENTLY = 7;
	}

	public static class Attributes{
		public static final String ID  = "id";
        public static final String MKEY  = "mkey";
        public static final String CKEY  = "ckey";
        public static final String FIRST_NAME  = "firstName";
        public static final String LAST_NAME  = "lastName";
        public static final String OUTGOING_VIDEO_ID = "outgoingVideoId";
        public static final String OUTGOING_VIDEO_STATUS  = "outgoingVideoStatus";
        public static final String UPLOAD_RETRY_COUNT  = "uploadRetryCount";
        public static final String LAST_VIDEO_STATUS_EVENT_TYPE = "lastVideoStatusEventType";
        public static final String TIME_OF_LAST_ACTION = "timeOfLastAction";
        public static final String MOBILE_NUMBER = "mobileNumber";
        public static final String HAS_APP = "hasApp";
    }

    public static class VideoStatusEventType{
        public static final Integer OUTGOING = 0;
        public static final Integer INCOMING = 1;
    }

    @Override
    public String[] attributeList() {
        final String[] a = {
                Attributes.ID,
                Attributes.MKEY,
                Attributes.CKEY,
                Attributes.FIRST_NAME,
                Attributes.LAST_NAME,
                Attributes.OUTGOING_VIDEO_ID,
                Attributes.OUTGOING_VIDEO_STATUS,
                Attributes.UPLOAD_RETRY_COUNT,
                Attributes.LAST_VIDEO_STATUS_EVENT_TYPE,
                Attributes.TIME_OF_LAST_ACTION,
                Attributes.MOBILE_NUMBER,
                Attributes.HAS_APP};
        return a;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        setOutgoingVideoStatus(OutgoingVideoStatus.NONE);
        set(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE, VideoStatusEventType.INCOMING.toString());
        setUploadRetryCount(0);
    }

    public String getLastActionTime() {
        return get(Friend.Attributes.TIME_OF_LAST_ACTION);
    }

    public void setLastActionTime(long lastActionTime){
        set(Friend.Attributes.TIME_OF_LAST_ACTION, Long.toString(lastActionTime));
    }

    public boolean hasApp() {
        return get(Attributes.HAS_APP).equals("true");
    }

    public void setHasApp(){
        set(Attributes.HAS_APP, "true");
    }
    
    public void setHasApp(boolean value){
        set(Attributes.HAS_APP, value ? "true" : "false");
    }
    
    //------------------------------
    // Attribute Getters and Setters
    //------------------------------
    public String getOutgoingVideoId(){
        return  get(Friend.Attributes.OUTGOING_VIDEO_ID);
    }
    
    public void setOutGoingVideoId(String videoId){
        set(Attributes.OUTGOING_VIDEO_ID, videoId);
    }

    //------------
    // PhoneNumber
    //------------
    public PhoneNumber getPhoneNumber() {
        PhoneNumberUtil pu = PhoneNumberUtil.getInstance();
        PhoneNumber pn = null;
        try {
            pn = pu.parse(get(Attributes.MOBILE_NUMBER), UserFactory.current_user().getRegion());
        } catch (NumberParseException e) {
            Dispatch.dispatch("ERROR: Could not get phone number object for friends phone this should never happen.");
        }
        return pn;
    }

    
    
    //================
    // Incoming Videos
    //================
    
    //-------------------
    // Create and destroy
    //-------------------
    public void createIncomingVideo(Context context, String videoId){
        if (hasIncomingVideoId(videoId))
            return;
        VideoFactory vf = VideoFactory.getFactoryInstance();
        IncomingVideo v = vf.makeInstance(context);
        v.set(IncomingVideo.Attributes.FRIEND_ID, getId());
        v.set(IncomingVideo.Attributes.ID, videoId);
    }

    public void deleteVideo(String videoId){
        // Delete videoFile
        videoFromFile(videoId).delete();
        // Delete video object
        VideoFactory vf = VideoFactory.getFactoryInstance();
        vf.delete(videoId);
    }

    public void deleteAllViewedVideos(){
        for (IncomingVideo v : getIncomingVideos()){
            if (v.getIncomingVideoStatus() == IncomingVideo.IncomingVideoStatus.VIEWED ||
                    v.getIncomingVideoStatus() == IncomingVideo.IncomingVideoStatus.FAILED_PERMANENTLY)
                deleteVideo(v.getId());
        }
    }

    public void deleteAllVideos(){
        for (IncomingVideo v : getIncomingVideos()){
            deleteVideo(v.getId());
        }
    }

    public synchronized void deleteThumb() {
        // Delete thumbFile
        thumbFile().delete();
    }
    //--------------------------
    // All incoming (Any status)
    //--------------------------
    public ArrayList<IncomingVideo> getIncomingVideos(){
        return VideoFactory.getFactoryInstance().allWithFriendId(getId());
    }
    
    public ArrayList<IncomingVideo> getSortedIncomingVideos(){
        return sortVideosByTimeStamp(getIncomingVideos());
    }
    
    public IncomingVideo oldestIncomingVideo(){
        ArrayList<IncomingVideo> vids = getSortedIncomingVideos();
        if (vids.isEmpty()){
            return null;
        }
        return getSortedIncomingVideos().get(0);
    }

    public IncomingVideo newestIncomingVideo(){
        ArrayList <IncomingVideo> vids = getSortedIncomingVideos();
        IncomingVideo v = null;
        if (!vids.isEmpty())
            v = vids.get(vids.size() -1);
        return v;
    }

    public boolean hasIncomingVideoId(String videoId){
        for (IncomingVideo v : getIncomingVideos()){
            if (v.getId().equals(videoId))
                return true;
        }
        return false;
    }
    
    
    //----------------------
    // Unviewed (DOWNLOADED)
    //----------------------    
    public ArrayList<IncomingVideo> getIncomingNotViewedVideos(){
        ArrayList<IncomingVideo> incomingVideos = getIncomingVideos();
        Iterator<IncomingVideo> i = incomingVideos.iterator();
        while (i.hasNext()){
            IncomingVideo v = i.next();
            if(v.getIncomingVideoStatus() != IncomingVideo.IncomingVideoStatus.DOWNLOADED)
                i.remove();
        }
        return incomingVideos;
    }
    
    public ArrayList<IncomingVideo> getSortedIncomingNotViewedVideos(){
        return sortVideosByTimeStamp(getIncomingNotViewedVideos());
    }
    
    public String getFirstUnviewedVideoId(){
        return getFirstVideoIdInList(getSortedIncomingNotViewedVideos());
    }
    
    public String getNextUnviewedVideoId(String videoId){
        return getNextVideoIdInList(videoId, getSortedIncomingNotViewedVideos());
    }
    
    
    
    //--------------------------------
    // Playable (DOWNLOADED || VIEWED)
    //--------------------------------
    public ArrayList<IncomingVideo> getIncomingPlayableVideos(){
        ArrayList<IncomingVideo> incomingVideos = getIncomingVideos();
        Iterator<IncomingVideo> i = incomingVideos.iterator();
        while (i.hasNext()){
            IncomingVideo v = i.next();
            if(v.getIncomingVideoStatus() != IncomingVideo.IncomingVideoStatus.DOWNLOADED && v.getIncomingVideoStatus() != IncomingVideo.IncomingVideoStatus.VIEWED)
                i.remove();
        }
        return incomingVideos;
    } 

    public boolean hasIncomingPlayableVideos() {
        ArrayList<IncomingVideo> incomingVideos = getIncomingVideos();
        Iterator<IncomingVideo> i = incomingVideos.iterator();
        while (i.hasNext()) {
            IncomingVideo v = i.next();
            if (v.getIncomingVideoStatus() == IncomingVideo.IncomingVideoStatus.DOWNLOADED ||
                    v.getIncomingVideoStatus() == IncomingVideo.IncomingVideoStatus.VIEWED) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<IncomingVideo> getSortedIncomingPlayableVideos(){
        Log.i(TAG, "getSortedIncomingPlayableVideos: " + sortVideosByTimeStamp(getIncomingPlayableVideos()));
        return sortVideosByTimeStamp(getIncomingPlayableVideos());
    }
    
    public String getFirstPlayableVideoId(){
        return getFirstVideoIdInList(getSortedIncomingPlayableVideos());
    }

    public String getNextPlayableVideoId(String videoId){
        return getNextVideoIdInList(videoId, getSortedIncomingPlayableVideos());
    }
    
    
    //--------------------------------
    // Private helpers for above lists
    //--------------------------------
    private String getNextVideoIdInList(String videoId, List<IncomingVideo> videoList){
        boolean found = false;
        for (IncomingVideo v : videoList){
            if (found) {
                return v.getId();
            }
            if (v.getId().equals(videoId))
                found = true;
        }
        // As videoList may not contain videoId at all, for example it gets deleted during playing,
        // or between stop and start methods of player we decided to play first item from the list if it is
        if (!found) {
            return getFirstVideoIdInList(videoList);
        }
        return null;
    }
    
    private String getFirstVideoIdInList(List<IncomingVideo> videoList){
        if(videoList.size()==0)
            return null;
        else 
            return videoList.get(0).getId();
    }
    
    private ArrayList<IncomingVideo> sortVideosByTimeStamp(ArrayList<IncomingVideo> videos){
        Collections.sort(videos, new IncomingVideo.VideoTimestampComparator());
        return videos;
    }

    //----------------
    // Video and thumb
    //----------------
    public String videoFromPath(String videoId) {
        return buildPath(videoId, "vid_from", MP4);
    }

    public File videoFromFile(String videoId) {
        return new File(videoFromPath(videoId));
    }

    public String videoToPath(String videoId) {
        return buildPath(videoId, "vid_to", MP4);
    }

    public File videoToFile(String videoId) {
        return new File(videoToPath(videoId));
    }

    public String thumbPath() {
        return buildPath(getId(), "thumb_from", PNG);
    }

    public File thumbFile(){
        return new File(thumbPath());
    }

    private String buildPath(String id, String prefix, String extension) {
        StringBuilder path = new StringBuilder(Config.homeDirPath(getContext()));
        path.append(File.separator).append(prefix);
        path.append("_").append(id);
        path.append(extension);
        return path.toString();
    }

    public synchronized Bitmap thumbBitmap(){
        return Convenience.bitmapWithFile(thumbFile());
    }

    public Bitmap sqThumbBitmap(){
        Bitmap sq = null;
        Bitmap thumbBmp = thumbBitmap();
        if (thumbBmp != null){
            sq = ThumbnailUtils.extractThumbnail(thumbBmp, thumbBmp.getWidth(), thumbBmp.getWidth());
            Log.i(TAG, "sqThumbBitmap: size = " + String.valueOf(sq.getByteCount()));
        }
        return sq;
    }

    public synchronized boolean thumbExists() {
        if (!thumbFile().exists()) {
            migrateLegacyThumbs();
        }
        return thumbFile().exists();
    }

    private void migrateLegacyThumbs() {
        Iterator<IncomingVideo> videos = getSortedIncomingVideos().iterator();
        while (videos.hasNext()) {
            IncomingVideo video = videos.next();
            File thumb = new File(buildPath(video.getId(), "thumb_from", MP4));
            if (thumb.exists()) {
                thumb.renameTo(thumbFile());
            }
        }
    }

    public boolean incomingVideoNotViewed(){
        // Return true if any of the incoming videos are status DOWNLOADED
        Iterator<IncomingVideo> iterator = getIncomingVideos().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getIncomingVideoStatus() == IncomingVideo.IncomingVideoStatus.DOWNLOADED){
                return true;
            }
        }
        return false;
    }

    public int incomingVideoNotViewedCount(){
        // Return true if any of the incoming videos are status DOWNLOADED
        int i = 0;
        Iterator<IncomingVideo> iterator = getIncomingVideos().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getIncomingVideoStatus() == IncomingVideo.IncomingVideoStatus.DOWNLOADED) {
                i++;
            }
        }
        return i;
    }

    public void setIncomingVideoViewed(String videoId){
        if (!hasIncomingVideoId(videoId)){
            Dispatch.dispatch(TAG + " setIncomingVideoViewed: ERROR: incoming video doesnt exist");
            return;
        }

        IncomingVideo v = VideoFactory.getFactoryInstance().find(videoId);
        v.setIncomingVideoStatus(IncomingVideo.IncomingVideoStatus.VIEWED);
    }

    //-------------
    // Create Thumb
    //-------------
    public synchronized boolean createThumb(String videoId){
        Log.i(TAG, "createThumb for friend=" + getUniqueName());
        boolean res = false;

        if( !videoFromFile(videoId).exists() || videoFromFile(videoId).length() == 0 ){
            Dispatch.dispatch("createThumb: no video file found for friend=" + getUniqueName());
            res = false;
        }else{
            try {
                String vidPath = videoFromPath(videoId);
                ThumbnailRetriever retriever = new ThumbnailRetriever();
                Bitmap thumb = retriever.getThumbnail(vidPath);
                FileOutputStream fos = null;
                try {
                    fos = FileUtils.openOutputStream(thumbFile());
                    thumb.compress(Bitmap.CompressFormat.PNG, 100, fos);
                } finally {
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {}
                    }
                }
                res = true;
            } catch (IOException | RuntimeException | ThumbnailRetriever.ThumbnailBrokenException e) {
                Dispatch.dispatch(e, "createThumb: " + e.getMessage() + e.toString());
                res = false;
            }
        }
        return res;
    }

    //--------------------------
    // Video upload and download
    //--------------------------

    public void uploadVideo(){
        Log.i(TAG, "uploadVideo. For friend=" + getUniqueName());

        setAndNotifyOutgoingVideoStatus(OutgoingVideoStatus.QUEUED);

        Intent i = new Intent(getContext(), FileUploadService.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoToPath(getOutgoingVideoId()));
        i.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, getOutgoingVideoId());
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, RemoteStorageHandler.outgoingVideoRemoteFilename(this));
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", RemoteStorageHandler.outgoingVideoRemoteFilename(this));
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        getContext().startService(i);
    }

    public void downloadVideo(String videoId){
        Log.i(TAG, "downloadVideo. friend=" + getUniqueName() + " videoId=" + videoId);

        setAndNotifyIncomingVideoStatus(videoId, IncomingVideo.IncomingVideoStatus.QUEUED);

        Intent i = new Intent(getContext(), FileDownloadService.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
        i.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoFromPath(videoId));
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, RemoteStorageHandler.incomingVideoRemoteFilename(this, videoId));
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", RemoteStorageHandler.incomingVideoRemoteFilename(this, videoId));
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        getContext().startService(i);
    }

    public void deleteRemoteVideo(String videoId){
        Intent i = new Intent(getContext(), FileDeleteService.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
        i.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoFromPath(videoId));
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, RemoteStorageHandler.incomingVideoRemoteFilename(this, videoId));
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", RemoteStorageHandler.incomingVideoRemoteFilename(this, videoId));
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        getContext().startService(i);
    }

    public void requestDownload(String videoId) {
        Intent intent = new Intent(getContext(), IntentHandlerService.class);
        intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
        intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, IncomingVideo.IncomingVideoStatus.NEW);
        intent.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
        intent.putExtra(IntentHandlerService.IntentParamKeys.FRIEND_ID, getId());

        getContext().startService(intent);
    }

    //=============
    // Video Status
    //=============

    //-----------------------------------------
    // Changes to status and local notification.
    //------------------------------------------


    // Outgoing video status
    private void setOutgoingVideoStatus(int status){
        set(Attributes.OUTGOING_VIDEO_STATUS, String.valueOf(status));
    }

    public int getOutgoingVideoStatus(){
        return Integer.parseInt(get(Attributes.OUTGOING_VIDEO_STATUS));
    }

    public void setAndNotifyOutgoingVideoStatus(int status){
        if (getOutgoingVideoStatus() != status){
            setOutgoingVideoStatus(status);
            if (status == OutgoingVideoStatus.NEW)
                setUploadRetryCount(0);
            setLastEventTypeOutgoing();
            FriendFactory.getFactoryInstance().notifyStatusChanged(this);
        }
    }

    // Upload retryCount
    private void setUploadRetryCount(int retryCount){
        set(Attributes.UPLOAD_RETRY_COUNT, String.valueOf(retryCount));
    }

    private int getUploadRetryCount(){
        return Integer.parseInt(get(Attributes.UPLOAD_RETRY_COUNT));
    }

    public void setAndNotifyUploadRetryCount(int retryCount){
        if (getUploadRetryCount() != retryCount){
            setUploadRetryCount(retryCount);
            setLastEventTypeOutgoing();
            FriendFactory.getFactoryInstance().notifyStatusChanged(this);
        }
    }

    // Incoming video status
    public void setAndNotifyIncomingVideoStatus(String videoId, int status){
        IncomingVideo v = VideoFactory.getFactoryInstance().find(videoId);
        if (v == null){
            Dispatch.dispatch(TAG + " setAndNotifyIncomingVideoStatus: ERROR: incoming video doesnt exist");
            return;
        }

        if (v.getIncomingVideoStatus() != status){
            v.setIncomingVideoStatus(status);
            if (status == IncomingVideo.IncomingVideoStatus.VIEWED)
                notifyServerVideoViewed(videoId);

            // Only notify the UI of changes in status to the last incoming video.
            if (newestIncomingVideo().getId().equals(videoId)){
                // We want to preserve previous status if last event type is incoming and status is VIEWED
                if (status != IncomingVideo.IncomingVideoStatus.VIEWED) {
                    setLastEventTypeIncoming();
                }
                FriendFactory.getFactoryInstance().notifyStatusChanged(this);
            }
        }
    }

    // Download retryCount
    //	private void setDownloadRetryCount(int retryCount){
    //		set(Attributes.DOWNLOAD_RETRY_COUNT, ((Integer) retryCount).toString());
    //	}
    //
    //	private int getDownloadRetryCount(){
    //		return Integer.parseInt(get(Attributes.DOWNLOAD_RETRY_COUNT));
    //	}

    public void setAndNotifyDownloadRetryCount(String videoId, int retryCount){
        IncomingVideo v = VideoFactory.getFactoryInstance().find(videoId);
        if (v == null){
            Dispatch.dispatch(TAG + " setAndNotifyIncomingVideoStatus: ERROR: incoming video doesnt exist");
            return;
        }

        if (v.getDownloadRetryCount() != retryCount){
            v.setDownloadRetryCount(retryCount);

            // Only notify the UI of changes in retry count of the last incoming video.
            if (newestIncomingVideo().getId().equals(videoId)){
                setLastEventTypeIncoming();
                FriendFactory.getFactoryInstance().notifyStatusChanged(this);
            }
        }
    }

    // LastVideoStatusEventType
    private void setLastEventTypeOutgoing(){
        set(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE, VideoStatusEventType.OUTGOING.toString());
    }

    private void setLastEventTypeIncoming(){
        set(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE, VideoStatusEventType.INCOMING.toString());
    }

    public int getLastEventType(){
        return Integer.parseInt(get(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE));
    }


    public int getIncomingVideoStatus(){
        IncomingVideo v = newestIncomingVideo();
        if (v == null)
            return -1;
        return v.getIncomingVideoStatus();
    }
    
    public boolean hasDownloadingVideo(){
        Iterator<IncomingVideo> iterator = getIncomingVideos().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getIncomingVideoStatus() == IncomingVideo.IncomingVideoStatus.DOWNLOADING)
                return true;
        }
        return false;
    }
    
    public boolean hasRetryingDownload(){
        Iterator<IncomingVideo> iterator = getIncomingVideos().iterator();
        while (iterator.hasNext()) {
            IncomingVideo v = iterator.next();
            if (v.getIncomingVideoStatus() == IncomingVideo.IncomingVideoStatus.DOWNLOADING && v.getDownloadRetryCount() > 0)
                return true;
        }
        return false;
    }

    //-------------------------------
    // HttpRequest notification of changes
    //-------------------------------
    private void notifyServerVideoViewed(String videoId) {
        Log.i(TAG, "notifyServerVideoViewed");
        // Update kv store
        RemoteStorageHandler.setRemoteIncomingVideoStatus(this, videoId, RemoteStorageHandler.StatusEnum.VIEWED);
        // Send notification
        NotificationHandler.sendForVideoStatusUpdate(this, videoId, NotificationHandler.StatusEnum.VIEWED);
    }


    //----------------------------
    // VideoStatus String for view
    //----------------------------
    public String getStatusString(){
        if (getLastEventType() == VideoStatusEventType.INCOMING){
            return incomingStatusStr();
        } else {
            return outgoingStatusString();
        }
    }

    private String outgoingStatusString(){
        int status = getOutgoingVideoStatus();
        int count = getUploadRetryCount();
        String sfn = shortFirstName();

        switch (status){
            case OutgoingVideoStatus.NEW:
                return sfn + " n...";
            case OutgoingVideoStatus.QUEUED:
                return sfn + " q...";
            case OutgoingVideoStatus.UPLOADING:
                if (count > 0){
                    return sfn + " r" + count + "...";
                } else {
                    return sfn + " u...";
                }
            case OutgoingVideoStatus.UPLOADED:
                return sfn + " .s..";
            case OutgoingVideoStatus.DOWNLOADED:
                return sfn + " ..p.";
            case OutgoingVideoStatus.VIEWED:
                return sfn + " v!";
            case OutgoingVideoStatus.FAILED_PERMANENTLY:
                return sfn + " e!";
        }
        return getUniqueName();
    }

    private String incomingStatusStr() {
        IncomingVideo v = newestIncomingVideo();
        if (v == null)
            return getUniqueName();

        int status = v.getIncomingVideoStatus();
        int count = v.getDownloadRetryCount();

        switch (status){
            case IncomingVideo.IncomingVideoStatus.NEW:
                return "Dwnld new";
            case IncomingVideo.IncomingVideoStatus.QUEUED:
                return "Dwnld q";
            case IncomingVideo.IncomingVideoStatus.DOWNLOADING:
                if (count > 0){
                    return "Dwnld r" + count;
                } else {
                    return "Dwnld...";
                }
            case IncomingVideo.IncomingVideoStatus.DOWNLOADED:
                return getUniqueName();
            case IncomingVideo.IncomingVideoStatus.VIEWED:
                return getUniqueName();
            case IncomingVideo.IncomingVideoStatus.FAILED_PERMANENTLY:
                return "Dwnld e!";
        }
        return getUniqueName();
    }

    //------
    // Names
    //------
    
    private String shortFirstName(){
        String fn = getUniqueName();
        int shortLen = Math.min(7, fn.length());
        return fn.substring(0, shortLen);
    }

    public String getDisplayName(){
        if (DebugConfig.getInstance(getContext()).isDebugEnabled())
            return getStatusString();
        else
            return getUniqueName();
    }
    
    public String getUniqueName(){
        if (otherHasSameFirstName())
            return getFirstInitialDotLast();
        else 
            return getFirstName();
    }
    
    private boolean otherHasSameFirstName(){
        for (Friend f : FriendFactory.getFactoryInstance().all()){
            if (f != this && f.getFirstName().equalsIgnoreCase(getFirstName()))
                return true;
        }
        return false;        
    }

    public String getFirstName(){
        return get(Attributes.FIRST_NAME);
    }
    
    public String getFirstInitialDotLast(){
        return get(Attributes.FIRST_NAME).charAt(0) + ". " + get(Attributes.LAST_NAME);
    }

    public String getFullName() {
        return get(Attributes.FIRST_NAME) + " " + get(Attributes.LAST_NAME);
    }
}
