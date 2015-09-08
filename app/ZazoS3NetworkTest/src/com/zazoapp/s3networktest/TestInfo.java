package com.zazoapp.s3networktest;

import android.os.Parcel;
import android.os.Parcelable;
import com.zazoapp.s3networktest.core.PreferencesHelper;
import com.zazoapp.s3networktest.network.FileTransferService;

/**
 * Created by skamenkovych@codeminders.com on 9/8/2015.
 */
public class TestInfo implements Parcelable {
    long tries;
    long uploaded;
    long uploadedFailed;
    long downloaded;
    long downloadedFailed;
    long deleted;
    ManagerService.TransferTask currentTask;
    int currentStatus;
    int retryCount;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(tries);
        dest.writeLong(uploaded);
        dest.writeLong(uploadedFailed);
        dest.writeLong(downloaded);
        dest.writeLong(downloadedFailed);
        dest.writeLong(deleted);
        dest.writeInt(currentTask.ordinal());
        dest.writeInt(currentStatus);
        dest.writeInt(retryCount);
    }

    public void clear() {
        tries = 0;
        uploaded = 0;
        uploadedFailed = 0;
        downloaded = 0;
        downloadedFailed = 0;
        deleted = 0;
        currentTask = ManagerService.TransferTask.WAITING;
        currentStatus = 0;
        retryCount = 0;
    }

    public static final Creator<TestInfo> CREATOR
            = new Creator<TestInfo>() {
        public TestInfo createFromParcel(Parcel in) {
            return new TestInfo(in);
        }

        public TestInfo[] newArray(int size) {
            return new TestInfo[size];
        }
    };

    private TestInfo(Parcel in) {
        tries = in.readLong();
        uploaded = in.readLong();
        uploadedFailed = in.readLong();
        downloaded = in.readLong();
        downloadedFailed = in.readLong();
        deleted = in.readLong();
        currentTask = ManagerService.TransferTask.values()[in.readInt()];
        currentStatus = in.readInt();
        retryCount = in.readInt();
    }

    TestInfo() {
        currentTask = ManagerService.TransferTask.WAITING;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Tries: ").append(tries).append("\n");
        builder.append("↑").append(uploaded).append(" ↓").append(downloaded).append(" ⊗").append(deleted);
        builder.append("\nFailed: ⇡").append(uploadedFailed).append(" ⇣").append(downloadedFailed);
        builder.append("\ncurrent: \t").append(currentTask).append("\nstatus: \t");
        switch (currentStatus) {
            case FileTransferService.Transfer.NEW:
                builder.append("new");
                break;
            case FileTransferService.Transfer.IN_PROGRESS:
                builder.append("progress");
                break;
            case FileTransferService.Transfer.FAILED:
                builder.append("failed");
                break;
            case FileTransferService.Transfer.FINISHED:
                builder.append("finished");
                break;
        }
        builder.append("\nretry: \t").append(retryCount);
        return builder.toString();
    }

    public String toShortString() {
        StringBuilder builder = new StringBuilder();
        builder.append(tries).append(" ↑").append(uploaded).append(" ↓").append(downloaded);
        builder.append(" ⇡").append(uploadedFailed).append(" ⇣").append(downloadedFailed);
        builder.append(" (").append(currentTask.getChar()).append(":").append(currentStatus).append(" ↻").append(retryCount).append(")");
        return builder.toString();
    }

    public void save(PreferencesHelper data) {
        if (data != null) {
            data.putString("tries", String.valueOf(tries));
            data.putString("uploaded", String.valueOf(uploaded));
            data.putString("uploadedFailed", String.valueOf(uploadedFailed));
            data.putString("downloaded", String.valueOf(downloaded));
            data.putString("downloadedFailed", String.valueOf(downloadedFailed));
            data.putString("deleted", String.valueOf(deleted));
        }
    }

    public void load(PreferencesHelper data) {
        if (data != null) {
            tries = Long.parseLong(data.getString("tries", "0"));
            uploaded = Long.parseLong(data.getString("uploaded", "0"));
            uploadedFailed = Long.parseLong(data.getString("uploadedFailed", "0"));
            downloaded = Long.parseLong(data.getString("downloaded", "0"));
            downloadedFailed = Long.parseLong(data.getString("downloadedFailed", "0"));
            deleted = Long.parseLong(data.getString("deleted", "0"));
        }
    }
}
