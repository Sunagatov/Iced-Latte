package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsTemporaryLinkReceiver;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class FileProvider {

    private final AwsTemporaryLinkReceiver awsTemporaryLinkReceiver;
    private final FileMetadataProvider fileMetadataProvider;


    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Optional<String> getRelatedObjectUrl(final UUID relatedObjectId) {
        return fileMetadataProvider.getFileMetadataDto(relatedObjectId)
                .map(awsTemporaryLinkReceiver::generatePresignedUrlAsString);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Map<UUID, String> getRelatedObjectUrls(final List<UUID> relatedObjectIds) {
        return fileMetadataProvider.getFileMetadataDtos(relatedObjectIds)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> awsTemporaryLinkReceiver.generatePresignedUrlAsString(entry.getValue())
                ));
    }
}
