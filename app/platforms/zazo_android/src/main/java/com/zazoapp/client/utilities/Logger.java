package com.zazoapp.client.utilities;

import android.content.Context;
import android.util.Log;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.debug.DebugConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {

    private static final boolean IS_NEED_TO_SAVE = true;

    private static FileWriter fileWriter;

    private static SimpleDateFormat timeFormat = new SimpleDateFormat("MM/dd HH:mm:ss.SSS", Locale.getDefault());
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public static void e(String string, String string2, Exception e) {
        Log.e(string, string2, e);
        if (IS_NEED_TO_SAVE)
            printToFile(string + "\t" + string2, e);
    }

    public static void e(String string, String string2) {
        Log.e(string, string2);
        if (IS_NEED_TO_SAVE)
            printToFile(string + "\t" + string2);
    }

    public static void e(String string) {
        Log.e("EMPTY",string);
        if (IS_NEED_TO_SAVE)
            printToFile(string);
    }

    public static void w(String string, String string2) {
        Log.w(string, string2);
        if (IS_NEED_TO_SAVE)
            printToFile(string + "\t" + string2);
    }

    public static void d(String string) {
        d("LOG", string);

    }

    public static void d(String tag2, String string) {
        if (DebugConfig.DEBUG_LOG) {
            Log.d(tag2, string);
        }
        if (IS_NEED_TO_SAVE)
            printToFile(tag2 + "\t" + string);
    }

    public static void i(String string, String string2) {
        Log.i(string, string2);
        if (IS_NEED_TO_SAVE)
            printToFile(string + "\t" + string2);
    }

    public static void i(String tag, String markers, int... params) {
        StringBuilder builder = new StringBuilder();
        String[] markerArray = (markers != null) ? markers.split(" ") : null;
        for (int i = 0; i < params.length; i++) {
            if (markerArray != null && markerArray.length == params.length) {
                builder.append(markerArray[i]).append(": ");
            }
            builder.append(params[i]);
            if (i < params.length - 1) {
                builder.append(", ");
            }
        }
        Log.i(tag, builder.toString());
        if (IS_NEED_TO_SAVE)
            printToFile(tag + "\t" + builder.toString());
    }

    private static void printToFile(CharSequence text) {
        printToFile(text, null);
    }

    private static void printToFile(CharSequence text, Exception exc) {
        File logFile = getLogFileForDate(new Date());
        if (logFile == null) {
            return;
        }
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            // BufferedWriter for performance, true to set append to file flag
            fileWriter = new FileWriter(logFile,
                    true);
            BufferedWriter buf = new BufferedWriter(fileWriter);
            buf.append(timeFormat.format(new Date())).append(": ").append(text);
            buf.newLine();

            if (exc != null) {
                PrintWriter pw = new PrintWriter(fileWriter);
                exc.printStackTrace(pw);
            }
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FileWriter getFileWriter() {
        return fileWriter;
    }

    public static CharSequence getLogsForDate(Date date) {
        File logFile = getLogFileForDate(date);
        if (logFile == null || !logFile.exists()) {
            return "";
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static File getLogFileForDate(Date date) {
        Context context = TbmApplication.getContext();
        if (context == null) {
            return null;
        }
        File logsFolder = new File(context.getCacheDir().getAbsolutePath() + File.separator + "logs");
        logsFolder.mkdirs();
        File logFile = new File(logsFolder,
                "log_" + dateFormat.format(date) + ".txt");
        // Remove old logs
        if (!logFile.exists()) {
            File[] logs = logsFolder.listFiles();
            Date weekAgo = new Date(date.getTime() - 604800000);
            for (File log : logs) {
                if (log.isFile() && weekAgo.getTime() > log.lastModified()) {
                    log.delete();
                }
            }
        }
        return logFile;
    }
}