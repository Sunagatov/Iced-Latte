package com.zufar.icedlatte.user.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import lombok.experimental.UtilityClass;

@UtilityClass
class AvatarContentTypes {

    static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    static String normalize(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) {
            return "";
        }
        int parametersStart = contentType.indexOf(';');
        String mediaType = parametersStart == -1 ? contentType : contentType.substring(0, parametersStart);
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }

    static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    static Optional<String> detect(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
            if (isJpeg(header)) {
                return Optional.of("image/jpeg");
            }
            if (isPng(header)) {
                return Optional.of("image/png");
            }
            if (isWebp(header)) {
                return Optional.of("image/webp");
            }
            return Optional.empty();
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private static boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] header) {
        return header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && (header[1] & 0xFF) == 0x50
                && (header[2] & 0xFF) == 0x4E
                && (header[3] & 0xFF) == 0x47
                && (header[4] & 0xFF) == 0x0D
                && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A
                && (header[7] & 0xFF) == 0x0A;
    }

    private static boolean isWebp(byte[] header) {
        return header.length >= 12
                && (header[0] & 0xFF) == 0x52
                && (header[1] & 0xFF) == 0x49
                && (header[2] & 0xFF) == 0x46
                && (header[3] & 0xFF) == 0x46
                && (header[8] & 0xFF) == 0x57
                && (header[9] & 0xFF) == 0x45
                && (header[10] & 0xFF) == 0x42
                && (header[11] & 0xFF) == 0x50;
    }
}
