package com.zufar.icedlatte.test.config;

import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;

/*
public class MinioContainerTest {

    private static MinioContainer minioContainer;
    private static MinioClient minioClient;

    @BeforeClass
    public static void setUp() {
        minioContainer = new MinioContainer();
        minioContainer.start();
        minioClient = minioContainer.getMinioClient();
    }

    @Test
    public void testMinioConnection() {
        try {
            // Попытка выполнить запрос к MinIOContainer
            boolean bucketExists = minioClient.bucketExists("test-bucket");
            if (!bucketExists) {
                fail("MinIOContainer bucket does not exist");
            }
        } catch (MinioException e) {
            fail("Error connecting to MinIOContainer: " + e.getMessage());
        }
    }
}

 */
