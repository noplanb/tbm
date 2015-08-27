package com.zazoapp.client.dispatch;

import android.content.Context;
import com.zazoapp.client.Config;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.model.IncomingVideoFactory;
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
        ArrayList<Friend> friends = FriendFactory.getFactoryInstance().all();
        ArrayList<IncomingVideo> incomingVideos = IncomingVideoFactory.getFactoryInstance().all();
        if (friends.size() > 0) {
            addRow(info, "Friends");
            addRow(info, "Name", "ID", "Has app", "IV !v count", "OV ID", "OV status", "Last event", "Thumb", "Downloading", "Deleted", "ConnCreat", "Ever sent");
        } else {
            addRow(info, "Friends", "NO FRIENDS");
        }
        for (Friend friend : friends) {
            List<String> list = new ArrayList<>();
            list.add(friend.getFullName());
            list.add(friend.getId());
            list.add(friend.hasApp() ? "+" : "No app");
            list.add(String.valueOf(friend.incomingVideoNotViewedCount()));
            list.add(friend.getOutgoingVideoId());
            list.add(String.valueOf(friend.getOutgoingVideoStatus()));
            list.add((friend.getLastEventType() == Friend.VideoStatusEventType.OUTGOING) ? "OUT" : "IN");
            list.add(friend.thumbExists() ? "+" : "No thumb");
            list.add(friend.hasDownloadingVideo() ? "Downloading" : "");
            list.add(friend.isDeleted() ? "Deleted" : "");
            list.add(friend.isConnectionCreator() ? "Creator" : "Target");
            list.add(friend.everSent() ? "Welcomed" : "");
            addRow(info, list.toArray(new String[list.size()]));
        }
        info.append("\n");
        if (incomingVideos.size() > 0) {
            addRow(info, "Videos");
            addRow(info, "ID", "IV status", "Exists", "Size");
        }
        for (Friend friend : friends) {
            incomingVideos = friend.getIncomingVideos();
            addRow(info, friend.getFullName());
            for (IncomingVideo video : incomingVideos) {
                List<String> list = new ArrayList<>();
                list.add(video.getId());
                list.add(String.valueOf(video.getVideoStatus()));
                File file = friend.videoFromFile(video.getId());
                list.add(String.valueOf(file.exists()));
                list.add(file.exists() ? String.valueOf(file.length()) : "");
                addRow(info, list.toArray(new String[list.size()]));
            }
        }
        info.append("\n");
        File file = new File(Config.homeDirPath(context));
        if (file.isDirectory()) {
            File[] list = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.startsWith("vid_from");
                }
            });
            for (File f : list) {
                addRow(info, f.getName());
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
        int maxLength = Math.max(80 / data.length, 12);
        for (int i = 0; i < data.length; i++) {
            dataBuilder.append("| %-").append(maxLength).append("s ");
            if (data[i].length() > maxLength) {
                data[i] = data[i].substring(0, maxLength);
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
        };
        StringBuilder builder = new StringBuilder();
        for (String[] strings : arr) {
            addRow(builder, strings);
        }
        System.out.println(builder.toString());
    }
}
