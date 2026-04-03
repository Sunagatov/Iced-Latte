package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataDeleter;
import com.zufar.icedlatte.filestorage.filemetadata.FileMetadataProvider;
import com.zufar.icedlatte.filestorage.aws.AwsObjectDeleter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class FileDeleter {

    private final AwsObjectDeleter awsObjectDeleter;
    private final FileMetadataProvider fileMetadataProvider;
    private final FileMetadataDeleter fileMetadataDeleter;

    public FileDeleter(@Autowired(required = false) AwsObjectDeleter awsObjectDeleter,
                       FileMetadataProvider fileMetadataProvider,
                       FileMetadataDeleter fileMetadataDeleter) {
        this.awsObjectDeleter = awsObjectDeleter;
        this.fileMetadataProvider = fileMetadataProvider;
        this.fileMetadataDeleter = fileMetadataDeleter;
        if (awsObjectDeleter == null) {
            log.info("storage.aws.disabled: file deletion from S3 will be skipped");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID relatedObjectId) {
        fileMetadataProvider.getFileMetadataDto(relatedObjectId)
                .ifPresent(fileMetadata -> {
                    if (awsObjectDeleter != null) {
                        awsObjectDeleter.deleteFile(fileMetadata);
                    } else {
                        log.debug("file.delete.skipped: reason=aws_not_configured");
                    }
                    fileMetadataDeleter.deleteByRelatedObjectId(relatedObjectId);
                    log.info("file.deleted: objectId={}", relatedObjectId);
                });
    }
}
