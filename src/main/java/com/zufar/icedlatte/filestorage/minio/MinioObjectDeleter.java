package com.zufar.icedlatte.filestorage.minio;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioObjectDeleter {

    private final AmazonS3 amazonS3;
    private final MinioProvider minioProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void deleteFile(FileMetadataDto fileMetadataDto) {
        final String bucketName = fileMetadataDto.bucketName();
        final String fileName = fileMetadataDto.fileName();
        try {
            amazonS3.deleteObject(bucketName, fileName);
        } catch (AmazonServiceException ase) {
            log.error("Minio couldn't process operation", ase);
            throw ase;
        } catch (SdkClientException sce) {
            log.error("Minio couldn't be contacted for a response", sce);
            throw sce;
        }
    }

    @Transactional
    public void deleteAllPackage(String bucketName) {
        List<FileMetadataDto> fileMetadataDtos = minioProvider.getProductImagesFromMinio(bucketName);
        fileMetadataDtos.stream()
                .forEach(this::deleteFile);
    }
}
