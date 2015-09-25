package com.zazoapp.client.model;

import android.content.Context;
import com.zazoapp.client.core.RemoteStorageHandler;
import com.zazoapp.client.network.HttpRequest;

import java.util.List;

public class IncomingVideo extends Video {

    /**
     * Normal state machine: QUEUED -> NEW <-> DOWNLOADING -> DOWNLOADED -(onViewed)-> VIEWED
     */
    public static class Status {
        public static final int NONE = 0;
        public static final int NEW = 1;
        public static final int QUEUED = 2;
        public static final int DOWNLOADING = 3;
        public static final int DOWNLOADED = 4;
        public static final int VIEWED = 5;
        public static final int FAILED_PERMANENTLY = 6;
        public static final int MARKED_FOR_DELETION = 7;

        public static String toShortString(int status) {
            switch (status) {
                case NONE: return "i0";
                case NEW: return "in";
                case QUEUED: return "iq";
                case DOWNLOADING: return "idg";
                case DOWNLOADED: return "id";
                case VIEWED: return "iv";
                case FAILED_PERMANENTLY: return "if";
                case MARKED_FOR_DELETION: return "im";
                default: return "";
            }
        }
    }

    public static class Attributes extends Video.Attributes {
        public static final String REMOTE_STATUS  = "remote_status";
    }

    private static class RemoteStatus {
        static final String EXISTS = "exists";
        static final String MARKED_FOR_DELETE = "marked_for_delete";
        static final String KV_DELETED = "kv_deleted";
        static final String DELETED = "deleted";
    }

    @Override
    public List<String> attributeList() {
        List<String> attributeList = super.attributeList();
        attributeList.add(Attributes.REMOTE_STATUS);
        return attributeList;
    }

    @Override
    public void init(Context context) {
        super.init(context);
        setVideoStatus(Status.NONE);
        setRemoteStatus(RemoteStatus.EXISTS);
        setRetryCount(0);
    }

    public boolean isDownloaded() {
        return getVideoStatus() == Status.DOWNLOADED || getVideoStatus() == Status.VIEWED;
    }

    public boolean isFailed() {
        return getVideoStatus() == Status.FAILED_PERMANENTLY;
    }

    public void markForDeletion() {
        setVideoStatus(Status.MARKED_FOR_DELETION);
        if (RemoteStatus.EXISTS.equals(get(Attributes.REMOTE_STATUS))) {
            setRemoteStatus(RemoteStatus.MARKED_FOR_DELETE);
        }
    }

    public void deleteFromRemote() {
        if (RemoteStatus.EXISTS.equals(get(Attributes.REMOTE_STATUS))) {
            setRemoteStatus(RemoteStatus.MARKED_FOR_DELETE);
        }
        handleRemoteDeletion();
    }

    public void handleRemoteDeletion() {
        switch (get(Attributes.REMOTE_STATUS)) {
            case RemoteStatus.MARKED_FOR_DELETE: {
                Friend friend = FriendFactory.getFactoryInstance().find(getFriendId());
                RemoteStorageHandler.deleteRemoteIncomingVideoId(friend, getId(), new HttpRequest.Callbacks() {
                    @Override
                    public void success(String response) {
                        setRemoteStatus(RemoteStatus.KV_DELETED);
                        handleRemoteDeletion();
                    }

                    @Override
                    public void error(String errorString) {
                    }
                });
            }
            break;
            case RemoteStatus.KV_DELETED: {
                // Note it is ok if deleting the file fails as s3 will clean itself up after a few days.
                Friend friend = FriendFactory.getFactoryInstance().find(getFriendId());
                friend.deleteRemoteVideo(getId());
                setRemoteStatus(RemoteStatus.DELETED);
            }
            break;
        }
    }

    private void setRemoteStatus(String remoteStatus) {
        set(Attributes.REMOTE_STATUS, remoteStatus);
    }

    public boolean isRemoteDeleted() {
        return RemoteStatus.DELETED.equals(get(Attributes.REMOTE_STATUS));
    }
}
