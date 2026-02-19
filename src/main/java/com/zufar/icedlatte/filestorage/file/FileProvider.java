package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsTemporaryLinkReceiver;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileProvider {

    private final AwsTemporaryLinkReceiver awsTemporaryLinkReceiver;
    private final FileMetadataProvider fileMetadataProvider;

    public FileProvider(@Autowired(required = false) AwsTemporaryLinkReceiver awsTemporaryLinkReceiver,
                        FileMetadataProvider fileMetadataProvider) {
        this.awsTemporaryLinkReceiver = awsTemporaryLinkReceiver;
        this.fileMetadataProvider = fileMetadataProvider;
    }


    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Optional<String> getRelatedObjectUrl(final UUID relatedObjectId) {
        if (awsTemporaryLinkReceiver == null) {
            log.warn("AWS not configured, returning empty URL for the requested object.");
            return Optional.empty();
        }
        return fileMetadataProvider.getFileMetadataDto(relatedObjectId)
                .map(awsTemporaryLinkReceiver::generatePresignedUrlAsString);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Map<UUID, String> getRelatedObjectUrls(final List<UUID> relatedObjectIds) {
        if (awsTemporaryLinkReceiver == null) {
            log.warn("AWS not configured, returning empty URLs for {} requested objects.", relatedObjectIds.size());
            return Map.of();
        }
        return fileMetadataProvider.getFileMetadataDtos(relatedObjectIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> awsTemporaryLinkReceiver.generatePresignedUrlAsString(entry.getValue())
                ));
    }
}
