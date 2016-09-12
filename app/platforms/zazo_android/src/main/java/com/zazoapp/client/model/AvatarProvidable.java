package com.zazoapp.client.model;

/**
 * Created by skamenkovych@codeminders.com on 9/5/2016.
 */
public interface AvatarProvidable<T extends ActiveModel & AvatarProvidable> {
    String getAvatarTimestamp();
    String getAvatarFileName(Avatar.ThumbnailType type);
    String getAvatarOption();
    String getAvatarFolder();
    Avatar<T> getAvatar();

    String AVATAR_TIMESTAMP = "avatar_timestamp";
    String USE_AS_THUMBNAIL = "use_as_thumbnail";
}
