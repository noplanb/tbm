package com.zazoapp.s3networktest.dispatch;

/*
 *  Copyright 2010 Kevin Gaudin
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Executes logcat commands and collects it's output.
 *
 * @author Kevin Gaudin
 *
 */
public class LogCatCollector {

    /**
     * Default number of latest lines kept from the logcat output.
     */
    private static final int DEFAULT_TAIL_COUNT = 100;
    public static final int DEFAULT_LOGCAT_LINES = 100;
    public static final int DEFAULT_BUFFER_SIZE_IN_BYTES = 8192;

    private static final String TAG = LogCatCollector.class.getSimpleName();

    /**
     * Executes the logcat command with arguments taken from
     *
     *
     * @param bufferName
     *            The name of the buffer to be read: "main" (default), "radio"
     *            or "events".
     * @return A {@link String} containing the latest lines of the output.
     *         Default is 100 lijava.lang.Stringnes, use "-t", "300" in
     *         if you want 300 lines.
     *         You should be aware that increasing this value causes a longer
     *         report generation time and a bigger footprint on the device data
     *         plan consumption.
     */
    public static String collectLogCat(String bufferName) {
        final int myPid = android.os.Process.myPid();
        String myPidStr = null;
        if (myPid > 0) {
            myPidStr = Integer.toString(myPid) +"):";
        }

        final List<String> commandLine = new ArrayList<String>();
        commandLine.add("logcat");
        if (bufferName != null) {
            commandLine.add("-b");
            commandLine.add(bufferName);
        }

        // "-t n" argument has been introduced in FroYo (API level 8). For
        // devices with lower API level, we will have to emulate its job.
        final int tailCount;
        final List<String> logcatArgumentsList = new ArrayList<String>(
                Arrays.asList(new String[] { "-t", Integer.toString(DEFAULT_LOGCAT_LINES), "-v", "time" }));

        final int tailIndex = logcatArgumentsList.indexOf("-t");
        if (tailIndex > -1 && tailIndex < logcatArgumentsList.size()) {
            tailCount = Integer.parseInt(logcatArgumentsList.get(tailIndex + 1));
            if (Build.VERSION.SDK_INT < 8) {
                logcatArgumentsList.remove(tailIndex + 1);
                logcatArgumentsList.remove(tailIndex);
                logcatArgumentsList.add("-d");
            }
        } else {
            tailCount = -1;
        }

        final LinkedList<String> logcatBuf = new BoundedLinkedList<String>(tailCount > 0 ? tailCount
                : DEFAULT_TAIL_COUNT);
        commandLine.addAll(logcatArgumentsList);

        try {
            final Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[commandLine.size()]));
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()),
                    DEFAULT_BUFFER_SIZE_IN_BYTES);

            Log.d(TAG, "Retrieving logcat output...");

            // Dump stderr to null
            new Thread(new Runnable() {
                public void run() {
                    try {
                        InputStream stderr = process.getErrorStream();
                        byte[] dummy = new byte[DEFAULT_BUFFER_SIZE_IN_BYTES];
                        while (stderr.read(dummy) >= 0)
                            ;
                    } catch (IOException e) {
                    }
                }
            }).start();

            while (true) {
                final String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                if (myPidStr == null || line.contains(myPidStr)) {
                    logcatBuf.add(line + "\n");
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "LogCatCollector.collectLogCat could not retrieve data.");
        }

        return logcatBuf.toString();
    }
}

