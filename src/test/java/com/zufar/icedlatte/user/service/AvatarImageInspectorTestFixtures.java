package com.zufar.icedlatte.user.service;

final class AvatarImageInspectorTestFixtures {

    private AvatarImageInspectorTestFixtures() {}

    static byte[] png(int width, int height) {
        byte[] bytes = new byte[24];
        bytes[0] = (byte) 0x89;
        bytes[1] = 0x50;
        bytes[2] = 0x4E;
        bytes[3] = 0x47;
        bytes[4] = 0x0D;
        bytes[5] = 0x0A;
        bytes[6] = 0x1A;
        bytes[7] = 0x0A;
        putInt(bytes, 16, width);
        putInt(bytes, 20, height);
        return bytes;
    }

    static byte[] jpeg(int width, int height) {
        byte[] bytes = new byte[21];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        bytes[3] = (byte) 0xC0;
        bytes[4] = 0;
        bytes[5] = 17;
        bytes[6] = 8;
        bytes[7] = (byte) (height >>> 8);
        bytes[8] = (byte) height;
        bytes[9] = (byte) (width >>> 8);
        bytes[10] = (byte) width;
        return bytes;
    }

    static byte[] webpVp8x(int width, int height) {
        byte[] bytes = new byte[30];
        bytes[0] = 0x52;
        bytes[1] = 0x49;
        bytes[2] = 0x46;
        bytes[3] = 0x46;
        bytes[8] = 0x57;
        bytes[9] = 0x45;
        bytes[10] = 0x42;
        bytes[11] = 0x50;
        bytes[12] = 0x56;
        bytes[13] = 0x50;
        bytes[14] = 0x38;
        bytes[15] = 0x58;
        bytes[16] = 10;
        putLittleEndian24(bytes, 24, width - 1);
        putLittleEndian24(bytes, 27, height - 1);
        return bytes;
    }

    private static void putInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    private static void putLittleEndian24(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >>> 8);
        bytes[offset + 2] = (byte) (value >>> 16);
    }
}
