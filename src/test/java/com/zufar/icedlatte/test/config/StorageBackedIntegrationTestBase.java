package com.zufar.icedlatte.test.config;

import java.net.URI;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
public abstract class StorageBackedIntegrationTestBase extends ContainerizedIntegrationTestBase {

    protected static final String PRODUCTS_BUCKET = "iced-latte-products";
    protected static final String USER_AVATAR_BUCKET = "iced-latte-users";
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";
    private static final String MINIO_REGION = "us-east-1";

    @MockitoBean
    protected JavaMailSender javaMailSender;

    @SuppressWarnings("resource")
    protected static final GenericContainer<?> MINIO =
            new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                    .withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
                    .withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
                    .withCommand("server", "/data", "--console-address", ":9001")
                    .withExposedPorts(9000, 9001);

    static {
        MINIO.start();
        ensureBucketExists(PRODUCTS_BUCKET);
        ensureBucketExists(USER_AVATAR_BUCKET);
    }

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.aws.enabled", () -> "true");
        registry.add("spring.aws.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("spring.aws.secret-key", () -> MINIO_SECRET_KEY);
        registry.add("spring.aws.region", () -> MINIO_REGION);
        registry.add("spring.aws.endpoint-url", StorageBackedIntegrationTestBase::minioEndpoint);
        registry.add("spring.aws.public-url-base", () -> "");
        registry.add("spring.aws.buckets.products", () -> PRODUCTS_BUCKET);
        registry.add("spring.aws.buckets.user-avatar", () -> USER_AVATAR_BUCKET);
    }

    private static void ensureBucketExists(String bucketName) {
        try (S3Client client = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)))
                .region(Region.of(MINIO_REGION))
                .endpointOverride(URI.create(minioEndpoint()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build()) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (BucketAlreadyOwnedByYouException _) {
            log.debug("storage.test.bucket.exists: {}", bucketName);
        } catch (S3Exception ex) {
            if (ex.statusCode() != 409) {
                throw ex;
            }
            log.debug("storage.test.bucket.exists: {}", bucketName);
        }
    }

    private static String minioEndpoint() {
        return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
    }
}
