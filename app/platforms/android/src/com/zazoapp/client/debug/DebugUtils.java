package com.zazoapp.client.debug;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.InputType;
import android.util.Pair;
import android.widget.EditText;
import com.google.gson.Gson;
import com.zazoapp.client.Config;
import com.zazoapp.client.core.PreferencesHelper;
import com.zazoapp.client.model.ActiveModelsHandler;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by skamenkovych@codeminders.com on 3/24/2015.
 */
public final class DebugUtils {
    private DebugUtils() {
    }

    private static final String BROKEN_VIDEO_NAME = "test_video_broken.mp4";

    public static File getBrokenVideoFile(Context context) {
        AssetManager am = context.getAssets();
        String path = Config.homeDirPath(context) + BROKEN_VIDEO_NAME;
        try {
            return Convenience.createFileFromAssets(am, path, BROKEN_VIDEO_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void makeBackup(Context context) {
        copy(Config.homeDirPath(context), backupFolder());
        PreferencesHelper p = new PreferencesHelper(context, DebugConfig.DEBUG_SETTINGS);
        Preferences preferences = new Preferences();
        preferences.prefs = p.getAll();
        Gson gson = new Gson();
        String json = gson.toJson(preferences);
        Convenience.saveJsonToFile(json, backupFolder() + File.separator + DebugConfig.DEBUG_SETTINGS + ".json");
        p = new PreferencesHelper(context);
        int version = p.getInt(ActiveModelsHandler.MODEL_VERSION_PREF, 1);
        preferences.prefs.clear();
        preferences.prefs.put(ActiveModelsHandler.MODEL_VERSION_PREF, Pair.create(Integer.class.getSimpleName(), String.valueOf(version)));
        json = gson.toJson(preferences);
        Convenience.saveJsonToFile(json, backupFolder() + File.separator + "zazo_preferences.json");
        DialogShower.showToast(context, "Saved");
    }

    public static void restoreBackup(Context context) {
        copy(backupFolder(), Config.homeDirPath(context));
        String json = Convenience.getJsonFromFile(backupFolder() + File.separator + DebugConfig.DEBUG_SETTINGS + ".json");
        if (json != null) {
            Gson gson = new Gson();
            Preferences preferences = gson.fromJson(json, Preferences.class);
            PreferencesHelper p = new PreferencesHelper(context, DebugConfig.DEBUG_SETTINGS);
            p.putAll(preferences.prefs);
            DebugConfig.getInstance(context).reloadPrefs();
        }
        json = Convenience.getJsonFromFile(backupFolder() + File.separator + "zazo_preferences.json");
        if (json != null) {
            Gson gson = new Gson();
            Preferences preferences = gson.fromJson(json, Preferences.class);
            PreferencesHelper p = new PreferencesHelper(context);
            p.putAll(preferences.prefs);
        }
    }

    private static String backupFolder() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "ZazoBackup" + File.separator;
    }

    public static void copy(String srcPath, String dstPath) {
        File srcFolder = new File(srcPath);
        File dstFolder = new File(dstPath);
        if (!srcFolder.exists()) {
            return;
        }
        if (!dstFolder.exists()) {
            dstFolder.mkdirs();
        } else {
            for (File file : dstFolder.listFiles()) {
                deleteRecursively(file);
            }
        }
        try {
            copyFolder(srcFolder, dstFolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File file1 : file.listFiles()) {
                deleteRecursively(file1);
            }
        }
        file.delete();
    }

    public static void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            //if directory not exists, create it
            if (!dest.exists()) {
                dest.mkdir();
            }

            //list all the directory contents
            String files[] = src.list();

            for (String file : files) {
                //construct the src and dest file structure
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                //recursive copy
                copyFolder(srcFile, destFile);
            }
        } else {
            //if file, then copy it
            //Use bytes stream to support all file types
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }

    public static void requestCode(Context context, final InputDialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Enter code");
        // Set up the input
        final EditText input = new EditText(context);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setCancelable(false);
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onReceive(input.getText().toString());
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onReceive("");
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public static void requestConfirm(Context context, String text, final InputDialogCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirmation");
        builder.setMessage(text);
        builder.setCancelable(false);
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onReceive("OK");
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                callback.onReceive(null);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public interface InputDialogCallback {
        void onReceive(String input);
    }

    public static class Preferences {
        Map<String, Pair<String, String>> prefs;
    }
}
