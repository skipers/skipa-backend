package com.skipers.skipa.domain.patentextract.infra;

import com.skipers.skipa.domain.patentextract.application.PatentExtractStorageService;
import com.skipers.skipa.domain.patentextract.exception.PatentExtractException;
import com.skipers.skipa.global.exception.ErrorCode;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class MinioPatentExtractStorageService implements PatentExtractStorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Value("${app.minio.region}")
    private String region;

    @Value("${app.minio.presigned-url-expiry-seconds}")
    private int presignedUrlExpirySeconds;

    @Override
    public String generateUploadPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Http.Method.PUT)
                    .bucket(bucket)
                    .region(region)
                    .object(objectKey)
                    .expiry(presignedUrlExpirySeconds)
                    .build());
        } catch (Exception e) {
            throw new PatentExtractException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .region(region)
                    .object(objectKey)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            String code = e.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code)) {
                return false;
            }
            throw new PatentExtractException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        } catch (Exception e) {
            throw new PatentExtractException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
