package com.zazoapp.client.model;

import android.content.Context;
import android.text.TextUtils;
import com.zazoapp.client.Config;

import java.io.File;

/**
 * Created by skamenkovych@codeminders.com on 6/3/2015.
 */
public final class ModelUpgradeHelper {
    private ModelUpgradeHelper() {}

    /**
     * Added version attribute to each model
     * VideoFactory renamed to IncomingVideoFactory
     * Added OutgoingVideoFactory to support multiple outgoing videos per friend
     * Friend model now contains only reference to last outgoing video
     */
    public static void upgradeTo2(ActiveModelsHandler handler, Context context) {
        String videoPath = Config.homeDirPath(context) + "/VideoFactory_saved_instances.json";
        String incomingVideoPath = Config.homeDirPath(context) + "/"+ IncomingVideoFactory.class.getSimpleName() + "_saved_instances.json";
        File videoFile = new File(videoPath);
        if (videoFile.exists()) {
            videoFile.renameTo(new File(incomingVideoPath));
        }
        ensureAll(handler);

        // migrate old outgoing video model
        for (Friend friend : FriendFactory.getFactoryInstance().all()) {
            String id = friend.getOutgoingVideoId();
            if (!TextUtils.isEmpty(id)) {
                int status = friend.getOutgoingVideoStatus();
                OutgoingVideo video = OutgoingVideoFactory.getFactoryInstance().makeInstance(context);
                video.setVideoStatus(status);
                video.set(Video.Attributes.ID, id);
                video.set(Video.Attributes.FRIEND_ID, friend.getId());
            }
        }
    }

    /**
     * Changes in Friend model:
     * 1. Added CONNECTION_CREATOR flag, default value true
     * 2. Added DELETED flag
     * 3. Added EVER_SENT flag
     * Added INVITEE flag to User model
     */
    public static void upgradeTo3(ActiveModelsHandler handler, Context context) {
        ensureAll(handler);
        for (Friend friend : FriendFactory.getFactoryInstance().all()) {
            friend.setDeleted(false);
            friend.setEverSent(OutgoingVideo.Status.isSent(friend.getOutgoingVideoStatus()));
        }
    }

    /**
     * Changes in IncomingVideo model
     * 1. Added attribute REMOTE_STATUS, default value EXIST
     * 2. Added 7th state MARKED_FOR_DELETION
     */
    public static void upgradeTo4(ActiveModelsHandler handler, Context context) {
        ensureAll(handler);
    }

    /**
     * Added attribute CID to Friend model:
     */
    public static void upgradeTo5(ActiveModelsHandler handler, Context context) {
         ensureAll(handler);
    }

    /**
     * Added attribute TRANSCRIPTION to Incoming video model
     */
    public static void upgradeTo6(ActiveModelsHandler handler, Context context) {
        ensureAll(handler);
    }

    private static void ensureAll(ActiveModelsHandler handler) {
        handler.ensureUser();
        handler.ensure(FriendFactory.getFactoryInstance());
        handler.ensure(IncomingVideoFactory.getFactoryInstance());
        handler.ensure(GridElementFactory.getFactoryInstance());
        handler.ensure(OutgoingVideoFactory.getFactoryInstance());
    }

}
