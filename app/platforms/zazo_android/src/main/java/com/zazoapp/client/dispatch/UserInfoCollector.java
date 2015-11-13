package com.zazoapp.client.dispatch;

import android.content.Context;
import com.zazoapp.client.Config;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingVideo;
import com.zazoapp.client.model.IncomingVideoFactory;
import com.zazoapp.client.model.OutgoingVideo;
import com.zazoapp.client.model.OutgoingVideoFactory;
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
            list.add(String.valueOf(friend.incomingVideoNotViewedCount()));
            list.add(friend.getOutgoingVideoId());
            list.add(OutgoingVideo.Status.toShortString(friend.getOutgoingVideoStatus()));
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
            incomingVideos = friend.getIncomingVideos();
            addRow(info, friend.getFullName(), "", "", "", "");
            for (IncomingVideo video : incomingVideos) {
                List<String> list = new ArrayList<>();
                list.add(video.getId());
                list.add(IncomingVideo.Status.toShortString(video.getVideoStatus()));
                list.add(String.valueOf(video.get(IncomingVideo.Attributes.REMOTE_STATUS)));
                File file = friend.videoFromFile(video.getId());
                list.add(String.valueOf(file.exists()));
                list.add(file.exists() ? String.valueOf(file.length()) : "");
                addRow(info, list.toArray(new String[list.size()]));
            }
        }
        if (OutgoingVideoFactory.getFactoryInstance().count() > 0) {
            info.append("\n");
            addRow(info, "Outgoing Videos", "", "", "");
            addRow(info, "ID", "Status", "Exists", "Size");
            ArrayList<OutgoingVideo> videos;
            for (Friend friend : friends) {
                videos = OutgoingVideoFactory.getFactoryInstance().allWhere(OutgoingVideo.Attributes.FRIEND_ID, friend.getId());
                addRow(info, friend.getFullName(), "", "", "");
                for (OutgoingVideo video : videos) {
                    List<String> list = new ArrayList<>();
                    list.add(video.getId());
                    list.add(OutgoingVideo.Status.toShortString(video.getVideoStatus()));
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
                    return filename.startsWith("vid_from") || filename.startsWith("vid_to");
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
