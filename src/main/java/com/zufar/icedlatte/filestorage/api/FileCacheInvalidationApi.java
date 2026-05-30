package com.zufar.icedlatte.filestorage.api;

public interface FileCacheInvalidationApi {

    void invalidate(String fileKey);
}
