package com.zufar.icedlatte.user.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class AvatarUploadProcessor {

    private final AvatarUploadSourceObjectValidator sourceObjectValidator;
    private final AvatarImageInspector imageInspector;

    @SuppressWarnings("unused")
    public AvatarUploadProcessingResult process(
            AvatarUploadSourceObject sourceObject, byte[] imageBytes, long maxBytes, long maxPixels) {
        ValidAvatarUploadSourceObject source = sourceObjectValidator.validate(sourceObject);
        AvatarImageInspection image =
                imageInspector.inspect(imageBytes, source.requestedContentType(), maxBytes, maxPixels);
        return new AvatarUploadProcessingResult(source, image);
    }
}
