package com.zazoapp.client.network;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.zazoapp.client.Config;
import com.zazoapp.client.Utils;
import com.zazoapp.client.network.aws.S3CredentialsGetter;
import com.zazoapp.client.network.aws.S3CredentialsStore;
import com.zazoapp.client.tests.FileDownloadServiceTest;
import com.zazoapp.client.tests.FileUploadServiceTest;
import com.zazoapp.client.ui.MainActivity;
import junit.framework.Assert;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.zazoapp.client.ui.MainActivityTest \
 * com.zazoapp.client.tests/android.test.InstrumentationTestRunner
 */
public class S3TransferTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private static final String TAG = S3TransferTest.class.getSimpleName();

    private static final String FILE_ID = "VID_20150317_124910";
    private static final String FILE_NAME_KEY = "Test_video_" + FILE_ID;
    private static final String FILE_NAME = "VID_20150317_124910.mp4";
    private static final String FILE_MD5 = "20e6926578c28a574a438967bbaccde8";

    public S3TransferTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getCredentials();
    }

    public void testUploading() throws Exception {
        Context context = getActivity();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        // DO NOT FORGET to uncomment service in PUT's AndroidManifest
        FileUploadServiceTest.setListener(new FileUploadServiceTest.UploadCallback() {
            @Override
            public void onUploaded() {
                atomicBoolean.set(true);
                latch.countDown();
            }

            @Override
            public void onFailed() {
                latch.countDown();
            }
        });

        Intent i = new Intent(context, FileUploadServiceTest.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, FILE_ID);
        i.putExtra(FileTransferService.IntentFields.MESSAGE_ID_KEY, FILE_ID);
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, makeFileForUpload(context));
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, FILE_NAME_KEY);
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", FILE_NAME_KEY);
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        context.startService(i);

        if (!latch.await(20, TimeUnit.SECONDS)) {
            throw new InterruptedException("Too long uploading");
        }
    }

    public void testDownloading() throws Exception {
        final MainActivity activity = getActivity();
        final String path = buildPath(activity);
        final AtomicInteger successCounter = new AtomicInteger();
        final AtomicInteger brokenCounter = new AtomicInteger();
        final AtomicBoolean finished = new AtomicBoolean();
        final int MAX_RUNS = 10000;
        final CountDownLatch latch = new CountDownLatch(MAX_RUNS);
        final long startTime = System.currentTimeMillis();
        final int TIME_MINUTES = 100;

        // DO NOT FORGET to uncomment service in PUT's AndroidManifest
        FileDownloadServiceTest.setListener(new FileDownloadServiceTest.DownloadCallback() {
            private String message;
            final Runnable postProgress = new Runnable() {
                @Override
                public void run() {
                    //activity.onShowProgressDialog(message, null);
                }
            };
            @Override
            public boolean onDownloaded() {
                if (checkMd5(path)) {
                    successCounter.incrementAndGet();
                } else {
                    brokenCounter.incrementAndGet();
                }
                latch.countDown();
                try {
                    preparePathForDownloading(path);
                } catch (Exception e) {
                    e.printStackTrace();
                    finished.set(true);
                }
                postProgress();
                return latch.getCount() > 0 && !finished.get();
            }

            @Override
            public boolean onFailed() {
                latch.countDown();
                postProgress();
                return latch.getCount() > 0 && !finished.get();
            }

            private int getTime() {
                return TIME_MINUTES - (int) ((System.currentTimeMillis() - startTime) / 60_000);
            }

            private void postProgress() {
                message = String.format("%d|%d|%d. Ends in ~%d minutes", successCounter.get(), latch.getCount(), brokenCounter.get(), getTime());
                activity.runOnUiThread(postProgress);
            }
        });

        startDownloading(activity);
        boolean result = latch.await(TIME_MINUTES, TimeUnit.MINUTES);
        finished.set(true);
        long runs = MAX_RUNS - latch.getCount();
        long success = successCounter.get();
        long broken = brokenCounter.get();
        String message = "Downloaded: " + success + " from " + runs + ", broken " + broken;
        Log.i(TAG, message);
        //activity.onDismissProgressDialog();
        if (runs < 10) {
            Assert.fail("Too few attempts");
        }
        if (success < broken * MAX_RUNS / 10) {
            Assert.fail(message);
        }
    }

    private void getCredentials() throws Exception {
        if (!S3CredentialsStore.getInstance(getActivity()).hasCredentials()) {
            final CountDownLatch latch = new CountDownLatch(1);
            new S3CredentialsGetter(getActivity()) {
                @Override
                protected void success() {
                    super.success();
                    latch.countDown();
                }
            };
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new InterruptedException("Credentials weren't got on time");
            }
        }
    }

    private String makeFileForUpload(Context context) throws Exception {
        String path = buildPath(context);
        AssetManager am = getInstrumentation().getContext().getAssets();
        File videoFile = Utils.createFileFromAssets(am, path, FILE_NAME);
        return path;
    }

    private String preparePathForDownloading(String path) throws Exception {
        File file = new File(path);
        if (file.exists()) {
            Assert.assertTrue(file.delete());
        }
        return path;
    }

    private void startDownloading(Context context) throws Exception {
        String path = buildPath(context);
        preparePathForDownloading(path);
        Intent i = new Intent(context, FileDownloadServiceTest.class);
        i.putExtra(FileTransferService.IntentFields.ID_KEY, FILE_ID);
        i.putExtra(FileTransferService.IntentFields.MESSAGE_ID_KEY, FILE_ID);
        i.putExtra(FileTransferService.IntentFields.FILE_PATH_KEY, path);
        i.putExtra(FileTransferService.IntentFields.FILE_NAME_KEY, FILE_NAME_KEY);
        // This is here so the old saving files on server vs s3 work
        Bundle params = new Bundle();
        params.putString("filename", FILE_NAME_KEY);
        i.putExtra(FileTransferService.IntentFields.PARAMS_KEY, params);
        context.startService(i);
    }

    private String buildPath(Context context) {
        return Config.homeDirPath(context) + File.separator + FILE_NAME;
    }

    private static boolean checkMd5(InputStream stream) {
        try {
            String hex = new String(Hex.encodeHex(DigestUtils.md5(stream)));
            return FILE_MD5.equals(hex);
        } catch (IOException e) {
        }
        return false;
    }

    private static boolean checkMd5(String path) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            return checkMd5(fis);
        } catch (FileNotFoundException e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
        }
        return false;
    }
}
