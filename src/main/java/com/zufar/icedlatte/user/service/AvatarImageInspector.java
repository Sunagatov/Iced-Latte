package com.zufar.icedlatte.user.service;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;

@Component
public class AvatarImageInspector {

    public AvatarImageInspection inspect(byte[] bytes, String requestedContentType, long maxBytes, long maxPixels) {
        if (bytes == null || bytes.length == 0) {
            throw new BadRequestException("Avatar upload image bytes are invalid.");
        }
        if (bytes.length > maxBytes) {
            throw new BadRequestException("Avatar upload must be at most " + maxBytes + " bytes.");
        }
        String detectedContentType = AvatarContentTypes.detect(bytes)
                .orElseThrow(() -> new BadRequestException("Avatar upload image bytes are invalid."));
        if (!detectedContentType.equals(requestedContentType)) {
            throw new InvalidAvatarFileTypeException(requestedContentType, AvatarContentTypes.ALLOWED_CONTENT_TYPES);
        }

        ImageDimensions dimensions = dimensions(bytes, detectedContentType)
                .orElseThrow(() -> new BadRequestException("Avatar upload image bytes are invalid."));
        if (dimensions.pixels() > maxPixels) {
            throw new BadRequestException("Avatar upload image dimensions are too large.");
        }
        return new AvatarImageInspection(detectedContentType, dimensions.width(), dimensions.height(), bytes.length);
    }

    private Optional<ImageDimensions> dimensions(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/png" -> pngDimensions(bytes);
            case "image/jpeg" -> jpegDimensions(bytes);
            case "image/webp" -> webpDimensions(bytes);
            default -> Optional.empty();
        };
    }

    private Optional<ImageDimensions> pngDimensions(byte[] bytes) {
        if (bytes.length < 24) {
            return Optional.empty();
        }
        return dimensions(unsignedInt(bytes, 16), unsignedInt(bytes, 20));
    }

    private Optional<ImageDimensions> jpegDimensions(byte[] bytes) {
        int offset = 2;
        while (offset + 8 < bytes.length) {
            if (u8(bytes, offset) != 0xFF) {
                return Optional.empty();
            }
            while (offset < bytes.length && u8(bytes, offset) == 0xFF) {
                offset++;
            }
            if (offset >= bytes.length) {
                return Optional.empty();
            }
            int marker = u8(bytes, offset++);
            if (marker == 0xD8 || marker == 0xD9) {
                continue;
            }
            if (offset + 2 > bytes.length) {
                return Optional.empty();
            }
            int segmentLength = unsignedShort(bytes, offset);
            if (segmentLength < 2 || offset + segmentLength > bytes.length) {
                return Optional.empty();
            }
            if (isStartOfFrame(marker)) {
                if (segmentLength < 7) {
                    return Optional.empty();
                }
                int height = unsignedShort(bytes, offset + 3);
                int width = unsignedShort(bytes, offset + 5);
                return dimensions(width, height);
            }
            offset += segmentLength;
        }
        return Optional.empty();
    }

    private Optional<ImageDimensions> webpDimensions(byte[] bytes) {
        if (bytes.length < 30) {
            return Optional.empty();
        }
        String chunkType = ascii(bytes, 12, 4);
        return switch (chunkType) {
            case "VP8X" -> dimensions(littleEndian24(bytes, 24) + 1, littleEndian24(bytes, 27) + 1);
            case "VP8L" -> webpLosslessDimensions(bytes);
            case "VP8 " -> webpLossyDimensions(bytes);
            default -> Optional.empty();
        };
    }

    private Optional<ImageDimensions> webpLosslessDimensions(byte[] bytes) {
        if (bytes.length < 25 || u8(bytes, 20) != 0x2F) {
            return Optional.empty();
        }
        int b1 = u8(bytes, 21);
        int b2 = u8(bytes, 22);
        int b3 = u8(bytes, 23);
        int b4 = u8(bytes, 24);
        int width = 1 + (((b2 & 0x3F) << 8) | b1);
        int height = 1 + (((b4 & 0x0F) << 10) | (b3 << 2) | ((b2 & 0xC0) >>> 6));
        return dimensions(width, height);
    }

    private Optional<ImageDimensions> webpLossyDimensions(byte[] bytes) {
        if (bytes.length < 30 || u8(bytes, 23) != 0x9D || u8(bytes, 24) != 0x01 || u8(bytes, 25) != 0x2A) {
            return Optional.empty();
        }
        int width = unsignedShortLittleEndian(bytes, 26) & 0x3FFF;
        int height = unsignedShortLittleEndian(bytes, 28) & 0x3FFF;
        return dimensions(width, height);
    }

    private Optional<ImageDimensions> dimensions(long width, long height) {
        if (width < 1 || height < 1 || width > Integer.MAX_VALUE || height > Integer.MAX_VALUE) {
            return Optional.empty();
        }
        return Optional.of(new ImageDimensions((int) width, (int) height));
    }

    private boolean isStartOfFrame(int marker) {
        return marker == 0xC0
                || marker == 0xC1
                || marker == 0xC2
                || marker == 0xC3
                || marker == 0xC5
                || marker == 0xC6
                || marker == 0xC7
                || marker == 0xC9
                || marker == 0xCA
                || marker == 0xCB
                || marker == 0xCD
                || marker == 0xCE
                || marker == 0xCF;
    }

    private int u8(byte[] bytes, int offset) {
        return bytes[offset] & 0xFF;
    }

    private long unsignedInt(byte[] bytes, int offset) {
        return ((long) u8(bytes, offset) << 24)
                | ((long) u8(bytes, offset + 1) << 16)
                | ((long) u8(bytes, offset + 2) << 8)
                | u8(bytes, offset + 3);
    }

    private int unsignedShort(byte[] bytes, int offset) {
        return (u8(bytes, offset) << 8) | u8(bytes, offset + 1);
    }

    private int unsignedShortLittleEndian(byte[] bytes, int offset) {
        return u8(bytes, offset) | (u8(bytes, offset + 1) << 8);
    }

    private int littleEndian24(byte[] bytes, int offset) {
        return u8(bytes, offset) | (u8(bytes, offset + 1) << 8) | (u8(bytes, offset + 2) << 16);
    }

    private String ascii(byte[] bytes, int offset, int length) {
        StringBuilder value = new StringBuilder(length);
        for (int index = offset; index < offset + length; index++) {
            value.append((char) u8(bytes, index));
        }
        return value.toString();
    }

    private record ImageDimensions(int width, int height) {

        long pixels() {
            return (long) width * height;
        }
    }
}
