package com.zufar.icedlatte.filestorage.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FileUrlResolverApi {

    Optional<String> findFileUrl(UUID relatedObjectId);

    Map<UUID, String> findFileUrls(List<UUID> relatedObjectIds);
}
