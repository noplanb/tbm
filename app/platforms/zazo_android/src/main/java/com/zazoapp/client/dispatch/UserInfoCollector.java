package com.zazoapp.client.dispatch;

import android.content.Context;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.core.Settings;
import com.zazoapp.client.debug.DebugConfig;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingMessage;
import com.zazoapp.client.model.IncomingMessageFactory;
import com.zazoapp.client.model.OutgoingMessage;
import com.zazoapp.client.model.OutgoingMessageFactory;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.NetworkConfig;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by skamenkovych@codeminders.com on 3/20/2015.
 */
public class UserInfoCollector {
    private static final String TAG = UserInfoCollector.class.getSimpleName();

    public static String collect(Context context) {
        StringBuilder info = new StringBuilder();
        User user = UserFactory.current_user();
        if (user != null) {
            info.append(user.toString()).append("\n\n");
        }
        PreferencesHelper appSettings = new PreferencesHelper(context);
        info.append(PreferencesHelper.toShortString("AppS", appSettings)).append("\n\n");
        PreferencesHelper debugSettings = new PreferencesHelper(context, DebugConfig.DEBUG_SETTINGS);
        info.append(PreferencesHelper.toShortString("DebugS", debugSettings)).append("\n\n");
        PreferencesHelper userSettings = new PreferencesHelper(context, Settings.FILE_NAME);
        info.append(PreferencesHelper.toShortString("UserS", userSettings)).append("\n\n");
        ArrayList<Friend> friends = FriendFactory.getFactoryInstance().all();
        ArrayList<IncomingMessage> incomingVideos = IncomingMessageFactory.getFactoryInstance().all();
        if (friends.size() > 0) {
            addRow(info, "Friends", "", "", "", "", "", "", "", "", "", "", "");
            addRow(info, "Name", "ID", "Has app", "IV !v #", "OV ID", "OV status", "Last event", "Thumb", "Dwnlding", "Deleted", "Connect.", "EverSent");
        } else {
            addRow(info, "Friends", "NO FRIENDS");
        }
        for (Friend friend : friends) {
            List<String> list = new ArrayList<>();
            list.add(friend.getFullName());
            list.add(friend.getId());
            list.add(friend.hasApp() ? "+" : "No app");
            list.add(String.valueOf(friend.incomingMessagesNotViewedCount()));
            list.add(friend.getOutgoingVideoId());
            list.add(OutgoingMessage.Status.toShortString(friend.getOutgoingVideoStatus()));
            list.add((friend.getLastEventType() == Friend.VideoStatusEventType.OUTGOING) ? "OUT" : "IN");
            list.add(friend.thumbExists() ? "+" : "No thumb");
            list.add(friend.hasDownloadingVideo() ? "+" : "");
            list.add(friend.isDeleted() ? "Deleted" : "");
            list.add(friend.isConnectionCreator() ? "Creator" : "Target");
            list.add(friend.everSent() ? "Welcomed" : "");
            addRow(info, list.toArray(new String[list.size()]));
        }
        info.append("\n");
        if (incomingVideos.size() > 0) {
            addRow(info, "Videos", "", "", "", "");
            addRow(info, "ID", "IV status", "Remote", "Exists", "Size");
        }
        for (Friend friend : friends) {
            incomingVideos = friend.getIncomingMessages();
            if (!incomingVideos.isEmpty()) {
                addRow(info, friend.getFullName(), "", "", "", "");
            }
            for (IncomingMessage video : incomingVideos) {
                List<String> list = new ArrayList<>();
                list.add(video.getId());
                list.add(IncomingMessage.Status.toShortString(video.getStatus()));
                list.add(String.valueOf(video.get(IncomingMessage.Attributes.REMOTE_STATUS)));
                File file = friend.videoFromFile(video.getId());
                list.add(String.valueOf(file.exists()));
                list.add(file.exists() ? String.valueOf(file.length()) : "");
                addRow(info, list.toArray(new String[list.size()]));
            }
        }
        if (OutgoingMessageFactory.getFactoryInstance().count() > 0) {
            info.append("\n");
            addRow(info, "Outgoing Videos", "", "", "");
            addRow(info, "ID", "Status", "Exists", "Size");
            ArrayList<OutgoingMessage> videos;
            for (Friend friend : friends) {
                videos = OutgoingMessageFactory.getFactoryInstance().allWhere(OutgoingMessage.Attributes.FRIEND_ID, friend.getId());
                addRow(info, friend.getFullName(), "", "", "");
                for (OutgoingMessage video : videos) {
                    List<String> list = new ArrayList<>();
                    list.add(video.getId());
                    list.add(OutgoingMessage.Status.toShortString(video.getStatus()));
                    File file = friend.videoToFile(video.getId());
                    list.add(String.valueOf(file.exists()));
                    list.add(file.exists() ? String.valueOf(file.length()) : "");
                    addRow(info, list.toArray(new String[list.size()]));
                }
            }
        }
        info.append("\n");
        File file = new File(Config.homeDirPath(context));
        if (file.isDirectory()) {
            File[] list = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.startsWith("vid_from") || filename.startsWith("vid_to") || filename.startsWith("aud_from") || filename.endsWith(".json");
                }
            });
            for (File f : list) {
                addRow(info, f.getName(), String.valueOf(f.length()));
            }
        }
        info.append("\n");
        info.append("\nUnlocked features\n");
        info.append(Features.retrieveFeaturesStatus(context));

        info.append("\n");
        info.append("\nConnection status\n");
        info.append(NetworkConfig.getConnectionStatus(context));
        return info.toString();
    }

    private static void addRow(StringBuilder out, String... data) {
        StringBuilder dataBuilder = new StringBuilder();
        int maxLength = Math.max(80 / data.length, 8);
        int maxFirstLength = Math.max(80 / data.length, 15);
        int length = maxFirstLength;
        for (int i = 0; i < data.length; i++) {
            if (i != 0) {
                length = maxLength;
            }
            dataBuilder.append("| %-").append(length).append("s ");
            if (data[i].length() > length) {
                data[i] = data[i].substring(0, length);
            }
        }
        dataBuilder.append("|\n");
        String formattedString = String.format(dataBuilder.toString(), data);
        out.append(formattedString.replaceAll(" ", "\u00A0"));
    }

    // for testing purposes
    private static void main(String[] args) {
        String[][] arr = new String[][] {
                {"", "Video", "Thumb"},
                {"John Smith", "", ""},
                {"123879879", "Y", "N"},
                {"123879834", "Y", "Y"},
                {"Nancy Scott", "", ""},
                {"234523463", "Y", "Y"},
                {"Very long string with many symbols and even more"},
                {"Another long string with many symbols and two columns", ""},
                {"First Name Last Name", "Param", "Param", "Param", "Param", "Param", "Param", "Param"}
        };
        StringBuilder builder = new StringBuilder();
        for (String[] strings : arr) {
            addRow(builder, strings);
        }
        System.out.println(builder.toString());
    }
}
