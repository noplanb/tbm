package com.zazoapp.client.model;

import android.content.Context;
import android.text.TextUtils;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.MessageType;

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
        String incomingVideoPath = Config.homeDirPath(context) + "/"+ IncomingMessageFactory.class.getSimpleName() + "_saved_instances.json";
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
                OutgoingMessage video = OutgoingMessageFactory.getFactoryInstance().makeInstance(context);
                video.setStatus(status);
                video.set(Message.Attributes.ID, id);
                video.set(Message.Attributes.FRIEND_ID, friend.getId());
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
            friend.setEverSent(OutgoingMessage.Status.isSent(friend.getOutgoingVideoStatus()));
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
     * Changes in IncomingVideo model
     * 1. Added attribute TRANSCRIPTION to Incoming video model
     * 2. Added 8th state DOWNLOADED (old DOWNLOADED renamed to READY_TO_VIEW)
     */
    public static void upgradeTo6(ActiveModelsHandler handler, Context context) {
        ensureAll(handler);
    }

    private static void ensureAll(ActiveModelsHandler handler) {
        handler.ensureUser();
        handler.ensure(FriendFactory.getFactoryInstance());
        handler.ensure(IncomingMessageFactory.getFactoryInstance());
        handler.ensure(GridElementFactory.getFactoryInstance());
        handler.ensure(OutgoingMessageFactory.getFactoryInstance());
    }

    /**
     * Changes in Video models
     * 1. Renamed *VideoFactory* to *MessageFactory*
     * 2. Added type attribute
     */
    public static void upgradeTo7(ActiveModelsHandler handler, Context context) {
        String oldPath = Config.homeDirPath(context) + "/IncomingVideoFactory_saved_instances.json";
        String newPath = Config.homeDirPath(context) + "/"+ IncomingMessageFactory.class.getSimpleName() + "_saved_instances.json";
        File videoFile = new File(oldPath);
        if (videoFile.exists()) {
            videoFile.renameTo(new File(newPath));
        }
        oldPath = Config.homeDirPath(context) + "/OutgoingVideoFactory_saved_instances.json";
        newPath = Config.homeDirPath(context) + "/"+ OutgoingMessageFactory.class.getSimpleName() + "_saved_instances.json";
        videoFile = new File(oldPath);
        if (videoFile.exists()) {
            videoFile.renameTo(new File(newPath));
        }
        ensureAll(handler);
        for (IncomingMessage message : IncomingMessageFactory.getFactoryInstance().all()) {
            if (TextUtils.isEmpty(message.getType())) {
                message.setType(MessageType.VIDEO);
            }
        }
    }

    /**
     * Changed in Friend model
     * Added Abilities attribute
     */
    public static void upgradeTo8(ActiveModelsHandler handler, Context context) {
        ensureAll(handler);
    }

    /**
     * Changed in Friend and User model
     * Added Avatar infos
     */
    public static void upgradeTo9(ActiveModelsHandler handler, Context context) {
        ensureAll(handler);
        for (Friend friend : FriendFactory.getFactoryInstance().all()) {
            if (TextUtils.isEmpty(friend.getAvatarOption())) {
                friend.set(AvatarProvidable.USE_AS_THUMBNAIL, Avatar.ThumbnailType.LAST_FRAME.optionName());
                friend.set(AvatarProvidable.AVATAR_TIMESTAMP, String.valueOf(0));
            }
        }
        for (User user : UserFactory.getFactoryInstance().all()) {
            if (TextUtils.isEmpty(user.getAvatarOption())) {
                user.set(AvatarProvidable.USE_AS_THUMBNAIL, Avatar.ThumbnailType.LAST_FRAME.optionName());
                user.set(AvatarProvidable.AVATAR_TIMESTAMP, String.valueOf(0));
            }
        }
    }
}
