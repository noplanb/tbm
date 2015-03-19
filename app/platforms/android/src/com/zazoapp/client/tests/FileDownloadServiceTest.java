package com.zazoapp.client.tests;

import android.content.Intent;
import com.zazoapp.client.model.Video;
import com.zazoapp.client.network.FileDownloadService;

/**
 * Created by skamenkovych@codeminders.com on 3/17/2015.
 */
public class FileDownloadServiceTest extends FileDownloadService {

    private static DownloadCallback listener;

    @Override
    public void reportStatus(Intent intent, int status) {
        if (listener == null)
            return;
        switch (status) {
            case Video.IncomingVideoStatus.FAILED_PERMANENTLY:
                if (listener.onFailed()) {
                    startService(intent);
                }
                break;
            case Video.IncomingVideoStatus.DOWNLOADED:
                if (listener.onDownloaded()) {
                    startService(intent);
                }
                break;
        }
    }

    public static void setListener(DownloadCallback callback) {
        listener = callback;
    }

    public interface DownloadCallback {
        /** notifies callback that file was downloaded
         * @return true if need to restart */
        boolean onDownloaded();
        /** notifies callback that file was failed to download
         * @return true if need to restart */
        boolean onFailed();
    }
}
