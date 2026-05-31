package com.zufar.icedlatte.filestorage.api;

import java.io.IOException;

public interface BucketIndexMaintenanceApi {

    boolean isEnabled();

    void storeDirectory(String bucketName, String directoryPath) throws IOException;

    void refreshBucketIndex(String bucketName);
}
