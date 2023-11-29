package com.zufar.icedlatte.user.endpoint;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final AmazonS3 amazonS3;

    private final String bucketName = "user-avatars"; // Your bucket name

    public void uploadUserAvatar(UUID userId, MultipartFile file) {
        String key = "avatars/" + userId + "/" + file.getOriginalFilename();
        try {
            amazonS3.putObject(bucketName, key, file.getInputStream(), null);
            String avatarUrl = amazonS3.getUrl(bucketName, key).toString();


        } catch (IOException e) {
            throw new RuntimeException("Error storing file", e);
        }
    }
}
