package com.zazoapp.client.model;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.annotation.StringDef;
import android.util.Log;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.MessageType;
import com.zazoapp.client.core.RemoteStorageHandler;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.multimedia.ThumbnailRetriever;
import com.zazoapp.client.network.FileDeleteService;
import com.zazoapp.client.network.FileDownloadService;
import com.zazoapp.client.network.FileTransferService;
import com.zazoapp.client.network.FileUploadService;
import com.zazoapp.client.network.MessageUploadService;
import com.zazoapp.client.notification.NotificationHandler;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.StringUtils;
import org.apache.commons.io.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Friend extends ActiveModel implements AvatarProvidable {

    private static final String MP4 = ".mp4";
    private static final String PCM = ".pcm";
    private static final String PNG = ".png";
    private static final String TXT = ".txt";

    public static final Friend EMPTY = new Friend();

    public static final String ABILITY_TEXT_MESSAGING = "text_messaging";

    @StringDef(value = {Friend.ABILITY_TEXT_MESSAGING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Ability {}

    public enum File {
        IN_VIDEO("vid_from", MP4),
        OUT_VIDEO("vid_to", MP4),
        IN_AUDIO("aud_from", PCM),
        IN_THUMB("thumb_from", PNG),
        IN_TEXT("text_from", TXT),
        OUT_TEXT("text_to", TXT);

        private String prefix;
        private String ext;

        File(String prefix, String ext) {
            this.prefix = prefix;
            this.ext = ext;
        }

        public java.io.File getFile(Friend f, String id) {
            return new java.io.File(f.buildPath(id, prefix, ext));
        }

        public String getPath(Friend f, String id) {
            return f.buildPath(id, prefix, ext);
        }

        public boolean delete(Friend f, String id) {
            return getFile(f, id).delete();
        }

        public boolean exists(Friend f, String id) {
            return getFile(f, id).exists();
        }
    }

    public interface VideoStatusChangedCallback {
        void onVideoStatusChanged(Friend friend);
    }

    public static class Attributes{
		public static final String ID  = "id";
        public static final String MKEY  = "mkey";
        public static final String CKEY  = "ckey";
        public static final String CID = "cid";
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
        public static final String ABILITIES = "abilities";
        public static final String AVATAR_TIMESTAMP = AvatarProvidable.AVATAR_TIMESTAMP;
        public static final String USE_AS_THUMBNAIL = AvatarProvidable.USE_AS_THUMBNAIL;
    }

    public static class VideoStatusEventType{
        public static final Integer OUTGOING = 0;
        public static final Integer INCOMING = 1;
    }

    private Avatar<Friend> avatar = new Avatar<>(this);

    @Override
    public List<String> attributeList() {
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
                Attributes.EVER_SENT,
                Attributes.CID,
                Attributes.ABILITIES,
                Attributes.AVATAR_TIMESTAMP,
                Attributes.USE_AS_THUMBNAIL
        };
        return new ArrayList<>(Arrays.asList(a));
    }

    @Override
    public boolean validate() {
        return notEmpty(getId()) && (notEmpty(getFirstName()) || notEmpty(get(Attributes.LAST_NAME))) && notEmpty(getMkey());
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        setOutgoingVideoStatus(OutgoingMessage.Status.NONE);
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

    public void setNewOutgoingMessage(String videoId, MessageType type) {
        set(Attributes.OUTGOING_VIDEO_ID, videoId);
        OutgoingMessage message = OutgoingMessageFactory.getFactoryInstance().makeInstance(getContext());
        message.set(OutgoingMessage.Attributes.ID, videoId);
        message.set(OutgoingMessage.Attributes.FRIEND_ID, getId());
        message.setType(type);
        OutgoingMessageFactory.getFactoryInstance().deleteAllSent(getId());
    }

    @Override
    public String getMkey() {
        return get(Attributes.MKEY);
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

    public String getCid() {
        return get(Attributes.CID);
    }

    //================
    // Incoming Videos
    //================
    
    //-------------------
    // Create and destroy
    //-------------------
    public void createIncomingVideo(Context context, String videoId){
        if (hasIncomingMessageId(videoId))
            return;
        IncomingMessageFactory vf = IncomingMessageFactory.getFactoryInstance();
        IncomingMessage v = vf.makeInstance(context);
        v.set(Message.Attributes.FRIEND_ID, getId());
        v.set(Message.Attributes.ID, videoId);
        v.setType(MessageType.VIDEO);
    }

    public void createIncomingText(Context context, String messageId, String text){
        if (hasIncomingMessageId(messageId))
            return;
        IncomingMessageFactory vf = IncomingMessageFactory.getFactoryInstance();
        IncomingMessage v = vf.makeInstance(context);
        v.set(Message.Attributes.FRIEND_ID, getId());
        v.set(Message.Attributes.ID, messageId);
        v.setType(MessageType.TEXT);
        Convenience.saveTextToFile(text, File.IN_TEXT.getPath(this, messageId));
    }

    public void deleteMessage(String messageId){
        // Delete videoFile
        File.IN_VIDEO.delete(this, messageId);
        File.IN_AUDIO.delete(this, messageId);
        File.IN_TEXT.delete(this, messageId);
        // Delete video object
        IncomingMessageFactory vf = IncomingMessageFactory.getFactoryInstance();
        vf.delete(messageId);
    }

    public void deleteAllViewedMessages(){
        for (IncomingMessage v : getIncomingMessages()) {
            switch (v.getStatus()) {
                case IncomingMessage.Status.VIEWED:
                case IncomingMessage.Status.FAILED_PERMANENTLY:
                    if (MessageType.TEXT.is(v.getType())) {
                        File.IN_TEXT.delete(this, v.getId());
                    } else {
                        File.IN_VIDEO.delete(this, v.getId());
                        File.IN_AUDIO.delete(this, v.getId());
                    }
                    v.markForDeletion();
                case IncomingMessage.Status.MARKED_FOR_DELETION:
                    v.deleteFromRemote();
                    if (v.isRemoteDeleted()) {
                        deleteMessage(v.getId());
                    }
                    break;
            }
        }
    }

    public void deleteAllIncoming(){
        for (IncomingMessage v : getIncomingMessages()){
            deleteMessage(v.getId());
        }
    }

    public synchronized void deleteThumb() {
        // Delete thumbFile
        thumbFile().delete();
    }
    //--------------------------
    // All incoming (Any status)
    //--------------------------
    public ArrayList<IncomingMessage> getIncomingMessages(){
        return IncomingMessageFactory.getFactoryInstance().allWithFriendId(getId());
    }
    
    public ArrayList<IncomingMessage> getSortedIncomingMessages(){
        return sortByTimeStamp(getIncomingMessages());
    }
    
    public IncomingMessage oldestIncomingMessage(){
        ArrayList<IncomingMessage> vids = getSortedIncomingMessages();
        if (vids.isEmpty()){
            return null;
        }
        return getSortedIncomingMessages().get(0);
    }

    public IncomingMessage newestIncomingMessage(){
        ArrayList <IncomingMessage> vids = getSortedIncomingMessages();
        IncomingMessage v = null;
        if (!vids.isEmpty())
            v = vids.get(vids.size() -1);
        return v;
    }

    public boolean hasIncomingMessageId(String id){
        for (IncomingMessage v : getIncomingMessages()){
            if (v.getId().equals(id))
                return true;
        }
        return false;
    }
    
    
    //----------------------
    // Unviewed (READY_TO_VIEW)
    //----------------------    
    public ArrayList<IncomingMessage> getIncomingNotViewedMessages(){
        ArrayList<IncomingMessage> messages = getIncomingMessages();
        Iterator<IncomingMessage> i = messages.iterator();
        while (i.hasNext()){
            IncomingMessage v = i.next();
            if(v.getStatus() != IncomingMessage.Status.READY_TO_VIEW)
                i.remove();
        }
        return messages;
    }
    
    public ArrayList<IncomingMessage> getSortedIncomingNotViewedMessages(){
        return sortByTimeStamp(getIncomingNotViewedMessages());
    }
    
    //--------------------------------
    // Playable (READY_TO_VIEW || VIEWED)
    //--------------------------------
    public ArrayList<IncomingMessage> getIncomingPlayableMessages(){
        ArrayList<IncomingMessage> messages = getIncomingMessages();
        Iterator<IncomingMessage> i = messages.iterator();
        while (i.hasNext()){
            IncomingMessage v = i.next();
            if(v.getStatus() != IncomingMessage.Status.READY_TO_VIEW && v.getStatus() != IncomingMessage.Status.VIEWED)
                i.remove();
        }
        return messages;
    } 

    public boolean hasIncomingPlayableMessages() {
        ArrayList<IncomingMessage> messages = getIncomingMessages();
        Iterator<IncomingMessage> i = messages.iterator();
        while (i.hasNext()) {
            IncomingMessage v = i.next();
            if (v.getStatus() == IncomingMessage.Status.READY_TO_VIEW ||
                    v.getStatus() == IncomingMessage.Status.VIEWED) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<IncomingMessage> getSortedIncomingPlayableMessages(){
        ArrayList<IncomingMessage> list = sortByTimeStamp(getIncomingPlayableMessages());
        Log.i(TAG, "getSortedIncomingPlayableMessages: " + list);
        return list;
    }
    
    //--------------------------------
    // Private helpers for above lists
    //--------------------------------
    private ArrayList<IncomingMessage> sortByTimeStamp(ArrayList<IncomingMessage> messages){
        Collections.sort(messages, new Message.TimestampComparator<IncomingMessage>());
        return messages;
    }

    //----------------
    // Video and thumb
    //----------------
    public String videoFromPath(String videoId) {
        return File.IN_VIDEO.getPath(this, videoId);
    }

    public String audioFromPath(String videoId) {
        return File.IN_AUDIO.getPath(this, videoId);
    }

    public java.io.File videoFromFile(String videoId) {
        return File.IN_VIDEO.getFile(this, videoId);
    }

    public String videoToPath(String videoId) {
        return File.OUT_VIDEO.getPath(this, videoId);
    }

    public java.io.File videoToFile(String videoId) {
        return File.OUT_VIDEO.getFile(this, videoId);
    }

    public String thumbPath() {
        return File.IN_THUMB.getPath(this, getId());
    }

    public java.io.File thumbFile(){
        return File.IN_THUMB.getFile(this, getId());
    }

    private String buildPath(String id, String prefix, String extension) {
        StringBuilder path = new StringBuilder(Config.homeDirPath(getContext()));
        path.append(java.io.File.separator).append(prefix);
        path.append("_").append(id);
        path.append(extension);
        return path.toString();
    }

    public synchronized Bitmap thumbBitmap(){
        return avatar.loadBitmap();
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
        return avatar.exists();
    }

    private void migrateLegacyThumbs() {
        Iterator<IncomingMessage> videos = getSortedIncomingMessages().iterator();
        while (videos.hasNext()) {
            IncomingMessage video = videos.next();
            java.io.File thumb = new java.io.File(buildPath(video.getId(), "thumb_from", MP4));
            if (thumb.exists()) {
                thumb.renameTo(thumbFile());
            }
        }
    }

    public boolean incomingMessagesNotViewed(){
        // Return true if any of the incoming videos are status READY_TO_VIEW
        Iterator<IncomingMessage> iterator = getIncomingMessages().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getStatus() == IncomingMessage.Status.READY_TO_VIEW){
                return true;
            }
        }
        return false;
    }

    public int incomingMessagesNotViewedCount(){
        // Return true if any of the incoming videos are status READY_TO_VIEW
        int i = 0;
        Iterator<IncomingMessage> iterator = getIncomingMessages().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getStatus() == IncomingMessage.Status.READY_TO_VIEW) {
                i++;
            }
        }
        return i;
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
                avatar.updateBitmap();
                res = true;
            } catch (IOException | RuntimeException | ThumbnailRetriever.ThumbnailBrokenException e) {
                Dispatch.dispatch(e, "createThumb: " + e.getMessage() + e.toString());
                res = false;
            }
        }
        return res;
    }

    //--------------------------
    // Message upload and download
    //--------------------------

    public void uploadMessage(String messageId) {
        Log.i(TAG, "uploadMessage. For friend=" + getUniqueName());
        setAndNotifyOutgoingVideoStatus(messageId, OutgoingMessage.Status.QUEUED);
        OutgoingMessage message = OutgoingMessageFactory.getFactoryInstance().find(messageId);
        if (message != null) {
            switch (MessageType.get(message)) {
                case VIDEO: {
                    Intent i = new Intent(getContext(), FileUploadService.class);
                    i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
                    i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoToPath(messageId));
                    i.putExtra(FileTransferService.IntentFields.MESSAGE_ID_KEY, messageId);
                    i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, RemoteStorageHandler.outgoingVideoRemoteFilename(this, messageId));
                    i.putExtra(FileTransferService.IntentFields.METADATA, FileTransferService.MetaData.getMetadata(messageId, UserFactory.getCurrentUserMkey(), getMkey(), videoToFile(messageId)));
                    // This is here so the old saving files on server vs s3 work
                    Bundle params = new Bundle();
                    params.putString("filename", RemoteStorageHandler.outgoingVideoRemoteFilename(this, messageId));
                    i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
                    getContext().startService(i);
                }
                    break;
                case TEXT: {
                    Intent i = new Intent(getContext(), MessageUploadService.class);
                    i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, File.OUT_TEXT.getPath(this, messageId));
                    i.putExtra(FileTransferService.IntentFields.MESSAGE_ID_KEY, messageId);
                    i.putExtra(FileTransferService.IntentFields.MESSAGE_TYPE, MessageType.TEXT.getName());
                    i.putExtra(FileTransferService.IntentFields.RECEIVER_MKEY, getMkey());
                    getContext().startService(i);
                }
                    break;
            }
        }
    }

    public void downloadVideo(String videoId){
        Log.i(TAG, "downloadVideo. friend=" + getUniqueName() + " videoId=" + videoId);

        setAndNotifyIncomingVideoStatus(videoId, IncomingMessage.Status.QUEUED);

        Intent i = new Intent(getContext(), FileDownloadService.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, getId());
        i.putExtra(FileTransferService.IntentFields.MESSAGE_ID_KEY, videoId);
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
        i.putExtra(FileTransferService.IntentFields.MESSAGE_ID_KEY, videoId);
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, videoFromPath(videoId));
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, RemoteStorageHandler.incomingVideoRemoteFilename(this, videoId));
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", RemoteStorageHandler.incomingVideoRemoteFilename(this, videoId));
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        getContext().startService(i);
    }

    public void requestDownload(String id, String type, String body) {
        Intent intent = new Intent(getContext(), IntentHandlerService.class);
        intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_DOWNLOAD);
        intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, IncomingMessage.Status.NEW);
        intent.putExtra(FileTransferService.IntentFields.MESSAGE_ID_KEY, id);
        intent.putExtra(IntentHandlerService.IntentParamKeys.FRIEND_ID, getId());
        intent.putExtra(NotificationHandler.DataKeys.CONTENT_TYPE, type);
        intent.putExtra(NotificationHandler.DataKeys.BODY, body);
        getContext().startService(intent);
    }

    public void requestUpload(String videoId) {
        Intent intent = new Intent(getContext(), IntentHandlerService.class);
        intent.putExtra(FileTransferService.IntentFields.TRANSFER_TYPE_KEY, FileTransferService.IntentFields.TRANSFER_TYPE_UPLOAD);
        intent.putExtra(FileTransferService.IntentFields.STATUS_KEY, OutgoingMessage.Status.NEW);
        intent.putExtra(FileTransferService.IntentFields.MESSAGE_ID_KEY, videoId);
        intent.putExtra(IntentHandlerService.IntentParamKeys.FRIEND_ID, getId());
        getContext().startService(intent);
    }
    //=============
    // Message Status
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

    public void setAndNotifyOutgoingVideoStatus(String videoId, int status) {
        OutgoingMessage video = OutgoingMessageFactory.getFactoryInstance().find(videoId);
        if (video != null && video.getStatus() != status) {
            video.setStatus(status);
            if (status == OutgoingMessage.Status.NEW) {
                video.setRetryCount(0);
            }
            if (getOutgoingVideoId().equals(videoId)) {
                setOutgoingVideoStatus(status);
                setLastEventTypeOutgoing();
                FriendFactory.getFactoryInstance().notifyStatusChanged(this);
            }
        }
    }

    private int getUploadRetryCount() {
        OutgoingMessage video = OutgoingMessageFactory.getFactoryInstance().find(getOutgoingVideoId());
        return (video != null) ? video.getRetryCount() : 0;
    }

    public void setAndNotifyUploadRetryCount(String videoId, int retryCount) {
        OutgoingMessage video = OutgoingMessageFactory.getFactoryInstance().find(videoId);
        if (video != null && video.getRetryCount() != retryCount) {
            video.setRetryCount(retryCount);
            if (getOutgoingVideoId().equals(videoId)) {
                setLastEventTypeOutgoing();
                if (DebugConfig.isDebugEnabled()) {
                    FriendFactory.getFactoryInstance().notifyStatusChanged(this);
                }
            }
        }
    }

    // Incoming video status
    public void setAndNotifyIncomingVideoStatus(String videoId, int status){
        IncomingMessage v = IncomingMessageFactory.getFactoryInstance().find(videoId);
        if (v == null){
            Dispatch.dispatch(TAG + " setAndNotifyIncomingVideoStatus: ERROR: incoming video doesnt exist");
            return;
        }

        if (v.getStatus() != status){
            v.setStatus(status);
            if (status == IncomingMessage.Status.VIEWED)
                notifyServerVideoViewed(v);

            // Only notify the UI of changes in status to the last incoming video.
            if (newestIncomingMessage().getId().equals(videoId)){
                // We want to preserve previous status if last event type is incoming and status is VIEWED
                if (status != IncomingMessage.Status.VIEWED) {
                    setLastEventTypeIncoming();
                }
                FriendFactory.getFactoryInstance().notifyStatusChanged(this);
            }
        }
    }

    // Download retryCount
    //	private void setRetryCount(int retryCount){
    //		set(Attributes.RETRY_COUNT, ((Integer) retryCount).toString());
    //	}
    //
    //	private int getRetryCount(){
    //		return Integer.parseInt(get(Attributes.RETRY_COUNT));
    //	}

    public void setAndNotifyDownloadRetryCount(String videoId, int retryCount){
        IncomingMessage v = IncomingMessageFactory.getFactoryInstance().find(videoId);
        if (v == null){
            Dispatch.dispatch(TAG + " setAndNotifyIncomingVideoStatus: ERROR: incoming video doesnt exist");
            return;
        }

        if (v.getRetryCount() != retryCount){
            v.setRetryCount(retryCount);

            // Only notify the UI of changes in retry count of the last incoming video.
            if (newestIncomingMessage().getId().equals(videoId)){
                setLastEventTypeIncoming();
                if (DebugConfig.isDebugEnabled()) {
                    FriendFactory.getFactoryInstance().notifyStatusChanged(this);
                }
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
        IncomingMessage v = newestIncomingMessage();
        if (v == null)
            return -1;
        return v.getStatus();
    }
    
    public boolean hasDownloadingVideo(){
        Iterator<IncomingMessage> iterator = getIncomingMessages().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getStatus() == IncomingMessage.Status.DOWNLOADING)
                return true;
        }
        return false;
    }
    
    public boolean hasRetryingDownload(){
        Iterator<IncomingMessage> iterator = getIncomingMessages().iterator();
        while (iterator.hasNext()) {
            IncomingMessage v = iterator.next();
            if (v.getStatus() == IncomingMessage.Status.DOWNLOADING && v.getRetryCount() > 0)
                return true;
        }
        return false;
    }

    //-------------------------------
    // HttpRequest notification of changes
    //-------------------------------
    private void notifyServerVideoViewed(Message message) {
        Log.i(TAG, "notifyServerVideoViewed");
        // Updates kv store and send notification
        RemoteStorageHandler.setRemoteIncomingVideoStatus(this, message.getId(), MessageType.get(message), RemoteStorageHandler.StatusEnum.VIEWED);
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
            case OutgoingMessage.Status.NEW:
                return "n... " + sfn;
            case OutgoingMessage.Status.QUEUED:
                return "q... " + sfn;
            case OutgoingMessage.Status.UPLOADING:
                if (count > 0) {
                    return "r" + count + ".. " + sfn;
                } else {
                    return "u... " + sfn;
                }
            case OutgoingMessage.Status.UPLOADED:
                return ".s.. " + sfn;
            case OutgoingMessage.Status.DOWNLOADED:
                return "..p. " + sfn;
            case OutgoingMessage.Status.VIEWED:
                return "v! " + sfn;
            case OutgoingMessage.Status.FAILED_PERMANENTLY:
                return "e! " + sfn;
        }
        return getUniqueName();
    }

    private String incomingStatusStr() {
        IncomingMessage v = newestIncomingMessage();
        if (v == null)
            return getUniqueName();

        int status = v.getStatus();
        int count = v.getRetryCount();

        switch (status){
            case IncomingMessage.Status.NEW:
                return "Dwnld new";
            case IncomingMessage.Status.QUEUED:
                return "Dwnld q";
            case IncomingMessage.Status.DOWNLOADING:
                if (count > 0){
                    return "Dwnld r" + count;
                } else {
                    return "Dwnld...";
                }
            case IncomingMessage.Status.DOWNLOADED:
                return "Extr...";
            case IncomingMessage.Status.READY_TO_VIEW:
                return getUniqueName();
            case IncomingMessage.Status.VIEWED:
                return getUniqueName();
            case IncomingMessage.Status.FAILED_PERMANENTLY:
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
        if (DebugConfig.isDebugEnabled())
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
        String firstName = getFirstName();
        String lastName = get(Attributes.LAST_NAME);
        if (notEmpty(firstName) && notEmpty(lastName)) {
            return firstName.charAt(0) + ". " + lastName;
        } else if (notEmpty(firstName)) {
            return firstName;
        } else {
            return lastName;
        }
    }

    public String getFullName() {
        return get(Attributes.FIRST_NAME) + " " + get(Attributes.LAST_NAME);
    }

    public String getInitials() {
        return StringUtils.getInitials(get(Attributes.FIRST_NAME), get(Attributes.LAST_NAME));
    }

    public void setAbilities(String abilities) {
        set(Attributes.ABILITIES, abilities);
    }

    public boolean hasAbility(@Ability String ability) {
        String abilities = get(Attributes.ABILITIES);
        return abilities != null && abilities.contains(ability);
    }

    public String getAvatarTimestamp() {
        return get(Attributes.AVATAR_TIMESTAMP);
    }

    @Override
    public String getAvatarFileName(Avatar.ThumbnailType type) {
        return File.IN_THUMB.getPath(this, getId())/*getMkey() + "_" + getAvatarTimestamp() + ".png"*/;
    }

    @Override
    public String getAvatarOption() {
        return get(Attributes.USE_AS_THUMBNAIL);
    }

    @Override
    public String getAvatarFolder() {
        return null;
    }

    @Override
    public Avatar<Friend> getAvatar() {
        return avatar;
    }
}
