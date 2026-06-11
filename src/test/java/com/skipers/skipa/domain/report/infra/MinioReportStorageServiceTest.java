package com.skipers.skipa.domain.report.infra;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MinioReportStorageServiceTest {

    @Test
    void generatePresignedUrlUsesConfiguredBucketAndReportKey() {
        MinioClient publicMinioClient = MinioClient.builder()
                .endpoint("https://minio.example.com")
                .credentials("minioadmin", "minioadmin")
                .build();
        MinioReportStorageService storageService = new MinioReportStorageService(publicMinioClient);
        ReflectionTestUtils.setField(storageService, "bucket", "skipa");
        ReflectionTestUtils.setField(storageService, "region", "us-east-1");
        ReflectionTestUtils.setField(storageService, "presignedUrlExpirySeconds", 600);

        String url = storageService.generatePresignedUrl("reports/8001/report.html");

        assertThat(url).startsWith("https://minio.example.com/skipa/reports/8001/report.html?");
        assertThat(url).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertThat(url).contains("X-Amz-Expires=600");
    }
}
