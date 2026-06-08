package com.skipers.skipa.domain.patentextract.application;

import com.skipers.skipa.domain.patentextract.dao.PatentExtractJobRepository;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractUploadUrlResponse;
import com.skipers.skipa.domain.patentextract.exception.PatentExtractException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatentExtractJobService {

    private final PatentExtractJobRepository patentExtractJobRepository;
    private final PatentExtractStorageService patentExtractStorageService;

    @Value("${app.minio.presigned-url-expiry-seconds}")
    private int presignedUrlExpirySeconds;

    @Transactional
    public PatentExtractUploadUrlResponse createUploadUrl() {
        PatentExtractJob job = patentExtractJobRepository.save(PatentExtractJob.createUploadPending());
        String objectKey = buildTemporaryPdfObjectKey(job.getId());
        job.assignObjectKey(objectKey);

        String uploadUrl = generateUploadPresignedUrl(objectKey);

        return PatentExtractUploadUrlResponse.of(job, uploadUrl, presignedUrlExpirySeconds);
    }

    private String buildTemporaryPdfObjectKey(Long extractJobId) {
        return "patents/extract-jobs/%d/patent.pdf".formatted(extractJobId);
    }

    private String generateUploadPresignedUrl(String objectKey) {
        try {
            return patentExtractStorageService.generateUploadPresignedUrl(objectKey);
        } catch (RuntimeException e) {
            throw new PatentExtractException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
