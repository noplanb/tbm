package com.zazoapp.s3networktest.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by skamenkovych@codeminders.com on 5/29/2015.
 */
public class TransferTasksHolder {
    private Set<String> downloadingIds = new HashSet<>();
    private Set<String> uploadingIds = new HashSet<>();

    public boolean removeDownloadId(String id) {
        return downloadingIds.remove(id);
    }

    public boolean addDownloadId(String id) {
        return downloadingIds.add(id);
    }

    public boolean removeUploadId(String id) {
        return uploadingIds.remove(id);
    }

    public boolean addUploadId(String id) {
        return uploadingIds.add(id);
    }
}
