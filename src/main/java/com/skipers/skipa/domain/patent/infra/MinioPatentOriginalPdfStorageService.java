package com.skipers.skipa.domain.patent.infra;

import com.skipers.skipa.domain.patent.application.PatentOriginalPdfStorageService;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.global.exception.ErrorCode;
import io.minio.CopyObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.SourceObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class MinioPatentOriginalPdfStorageService implements PatentOriginalPdfStorageService {

    private final MinioClient minioClient;

    public MinioPatentOriginalPdfStorageService(@Qualifier("publicMinioClient") MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Value("${app.minio.bucket}")
    private String bucket;

    @Value("${app.minio.region}")
    private String region;

    @Value("${app.minio.presigned-url-expiry-seconds}")
    private int presignedUrlExpirySeconds;

    @Override
    public void copy(String sourceObjectKey, String targetObjectKey) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucket)
                    .region(region)
                    .object(targetObjectKey)
                    .source(SourceObject.builder()
                            .bucket(bucket)
                            .region(region)
                            .object(sourceObjectKey)
                            .build())
                    .build());
        } catch (Exception e) {
            throw new PatentException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    @Override
    public String generatePresignedUrl(String originalPdfKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Http.Method.GET)
                    .bucket(bucket)
                    .region(region)
                    .object(originalPdfKey)
                    .expiry(presignedUrlExpirySeconds)
                    .build());
        } catch (Exception e) {
            throw new PatentException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
