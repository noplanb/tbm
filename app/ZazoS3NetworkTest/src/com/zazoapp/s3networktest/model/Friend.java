package com.zazoapp.s3networktest.model;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.util.Log;
import com.zazoapp.s3networktest.Config;
import com.zazoapp.s3networktest.Convenience;
import com.zazoapp.s3networktest.network.FileDeleteService;
import com.zazoapp.s3networktest.network.FileDownloadService;
import com.zazoapp.s3networktest.network.FileTransferService;
import com.zazoapp.s3networktest.network.FileUploadService;

import java.io.File;

public class Friend extends ActiveModel {

    private static final String MP4 = ".mp4";
    private static final String PNG = ".png";

    public interface VideoStatusChangedCallback {
        void onVideoStatusChanged(Friend friend);
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
        public static final String CONNECTION_CREATOR = "isConnectionCreator";
        public static final String DELETED = "deleted";
        public static final String EVER_SENT = "everSent";
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
                Attributes.HAS_APP,
                Attributes.CONNECTION_CREATOR,
                Attributes.DELETED,
                Attributes.EVER_SENT};
        return a;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        setOutgoingVideoStatus(-1);
        setConnectionCreator(true);
        set(Attributes.LAST_VIDEO_STATUS_EVENT_TYPE, VideoStatusEventType.INCOMING.toString());
    }

    public String getLastActionTime() {
        return get(Friend.Attributes.TIME_OF_LAST_ACTION);
    }

    public void setLastActionTime(){
        set(Friend.Attributes.TIME_OF_LAST_ACTION, Long.toString(System.currentTimeMillis()));
    }

    public boolean hasApp() {
        return get(Attributes.HAS_APP).equals(TRUE);
    }

    public void setHasApp(){
        set(Attributes.HAS_APP, TRUE);
    }

    public void setHasApp(boolean value){
        set(Attributes.HAS_APP, value ? TRUE : FALSE);
    }

    public boolean isDeleted() {
        return get(Attributes.DELETED).equals(TRUE);
    }

    public void setDeleted(boolean value) {
        set(Attributes.DELETED, value ? TRUE : FALSE);
    }

    public boolean isConnectionCreator() {
        return TRUE.equals(get(Attributes.CONNECTION_CREATOR));
    }

    public void setConnectionCreator(boolean value) {
        set(Attributes.CONNECTION_CREATOR, value ? TRUE : FALSE);
    }

    public boolean everSent() {
        return TRUE.equals(get(Attributes.EVER_SENT));
    }

    public void setEverSent(boolean value) {
        set(Attributes.EVER_SENT, value ? TRUE : FALSE);
    }

    //------------------------------
    // Attribute Getters and Setters
    //------------------------------
    public String getOutgoingVideoId() {
        return get(Friend.Attributes.OUTGOING_VIDEO_ID);
    }

    public void setNewOutgoingVideoId(String videoId) {
        set(Attributes.OUTGOING_VIDEO_ID, videoId);
    }

    public String getMkey() {
        return get(Attributes.MKEY);
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
        return thumbFile().exists();
    }
    //
    ////-------------
    //// Create Thumb
    ////-------------
    //public synchronized boolean createThumb(String videoId){
    //    Log.i(TAG, "createThumb for friend=" + getUniqueName());
    //    boolean res = false;
    //
    //    if( !videoFromFile(videoId).exists() || videoFromFile(videoId).length() == 0 ){
    //        Dispatch.dispatch("createThumb: no video file found for friend=" + getUniqueName());
    //        res = false;
    //    }else{
    //        try {
    //            String vidPath = videoFromPath(videoId);
    //            ThumbnailRetriever retriever = new ThumbnailRetriever();
    //            Bitmap thumb = retriever.getThumbnail(vidPath);
    //            FileOutputStream fos = null;
    //            try {
    //                fos = FileUtils.openOutputStream(thumbFile());
    //                thumb.compress(Bitmap.CompressFormat.PNG, 100, fos);
    //            } finally {
    //                if (fos != null) {
    //                    try {
    //                        fos.close();
    //                    } catch (IOException e) {}
    //                }
    //            }
    //            res = true;
    //        } catch (IOException | RuntimeException | ThumbnailRetriever.ThumbnailBrokenException e) {
    //            Dispatch.dispatch(e, "createThumb: " + e.getMessage() + e.toString());
    //            res = false;
    //        }
    //    }
    //    return res;
    //}

    //--------------------------
    // Video upload and download
    //--------------------------

    public void uploadVideo(String videoId) {
        setAndNotifyOutgoingVideoStatus(videoId, FileTransferService.Transfer.NEW);

        Intent i = new Intent(getContext(), FileUploadService.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoToPath(videoId));
        i.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, videoId);
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", videoId);
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        getContext().startService(i);
    }

    public void setAndNotifyOutgoingVideoStatus(String videoId, int status) {
        setNewOutgoingVideoId(videoId);
        setOutgoingVideoStatus(status);
    }

    public void downloadVideo(String videoId){
        setAndNotifyIncomingVideoStatus(videoId, FileTransferService.Transfer.NEW);

        Intent i = new Intent(getContext(), FileDownloadService.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
        i.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoFromPath(videoId));
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, videoId);
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", videoId);
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        getContext().startService(i);
    }

    public void setAndNotifyIncomingVideoStatus(String videoId, int status) {
        setNewOutgoingVideoId(videoId);
        setOutgoingVideoStatus(status);
    }

    public void deleteRemoteVideo(String videoId){
        Intent i = new Intent(getContext(), FileDeleteService.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
        i.putExtra(FileTransferService.IntentFields.VIDEO_ID_KEY, videoId);
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoFromPath(videoId));
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, videoId);
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", videoId);
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        getContext().startService(i);
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


    // Download retryCount
    //	private void setRetryCount(int retryCount){
    //		set(Attributes.RETRY_COUNT, ((Integer) retryCount).toString());
    //	}
    //
    //	private int getRetryCount(){
    //		return Integer.parseInt(get(Attributes.RETRY_COUNT));
    //	}

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
