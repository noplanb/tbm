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
     * 1. Added CONNECTION CREATOR flag
     * 2. Added DELETED flag
     */
    public static void upgradeTo3(ActiveModelsHandler handler, Context context) {
        ensureAll(handler);
        for (Friend friend : FriendFactory.getFactoryInstance().all()) {
            friend.setCreator(true);
            friend.setDeleted(false);
        }
    }

    private static void ensureAll(ActiveModelsHandler handler) {
        handler.ensureUser();
        handler.ensure(FriendFactory.getFactoryInstance());
        handler.ensure(IncomingVideoFactory.getFactoryInstance());
        handler.ensure(GridElementFactory.getFactoryInstance());
        handler.ensure(OutgoingVideoFactory.getFactoryInstance());
    }
}
