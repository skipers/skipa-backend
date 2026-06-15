package com.skipers.skipa.domain.patent.infra;

import com.skipers.skipa.domain.patent.application.PatentOriginalPdfStorageService;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.global.exception.ErrorCode;
import io.minio.CopyObjectArgs;
import io.minio.MinioClient;
import io.minio.SourceObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class MinioPatentOriginalPdfStorageService implements PatentOriginalPdfStorageService {

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Value("${app.minio.region}")
    private String region;

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
}
