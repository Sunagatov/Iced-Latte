package com.zufar.icedlatte.user.endpoint;

import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.api.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = UserFileUploadEndpoint.API_CUSTOMERS)
public class UserFileUploadEndpoint {

    public static final String API_CUSTOMERS = "/api/v1/users";

    private final FileStorageService fileStorageService;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @PostMapping(path = "/avatar", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<Void> uploadUserAvatar(@RequestParam(value = "file") MultipartFile file) {
        UUID userId = securityPrincipalProvider.getUserId();
        fileStorageService.uploadUserAvatar(userId, file);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
