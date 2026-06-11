package com.skipers.skipa.domain.patentextract.infra;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MinioPatentExtractStorageServiceTest {

    @Test
    void generateUploadPresignedUrlUsesConfiguredBucketAndTemporaryObjectKey() {
        MinioClient internalMinioClient = MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadmin")
                .build();
        MinioClient publicMinioClient = MinioClient.builder()
                .endpoint("https://minio.example.com")
                .credentials("minioadmin", "minioadmin")
                .build();
        MinioPatentExtractStorageService storageService = new MinioPatentExtractStorageService(
                internalMinioClient,
                publicMinioClient
        );
        ReflectionTestUtils.setField(storageService, "bucket", "skipa");
        ReflectionTestUtils.setField(storageService, "region", "us-east-1");
        ReflectionTestUtils.setField(storageService, "presignedUrlExpirySeconds", 600);

        String url = storageService.generateUploadPresignedUrl("tmp/patent-extract-jobs/1/original.pdf");

        assertThat(url).startsWith("https://minio.example.com/skipa/tmp/patent-extract-jobs/1/original.pdf?");
        assertThat(url).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertThat(url).contains("X-Amz-Expires=600");
    }
}
