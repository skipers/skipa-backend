package com.skipers.skipa.domain.report.infra;

import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.report.exception.ReportException;
import com.skipers.skipa.global.exception.ErrorCode;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioReportStorageService implements ReportStorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Value("${app.minio.region}")
    private String region;

    @Value("${app.minio.presigned-url-expiry-seconds}")
    private int presignedUrlExpirySeconds;

    @Override
    public String generatePresignedUrl(String reportKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Http.Method.GET)
                    .bucket(bucket)
                    .region(region)
                    .object(reportKey)
                    .expiry(presignedUrlExpirySeconds)
                    .build());
        } catch (Exception e) {
            throw new ReportException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
