package com.skipers.skipa.domain.patentextract.application;

import com.skipers.skipa.domain.patentextract.dao.PatentExtractJobRepository;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractJobStatusResponse;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractResultResponse;
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
    private final PatentExtractPublisher patentExtractPublisher;

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

    @Transactional
    public PatentExtractJobStatusResponse completeUpload(Long extractJobId) {
        PatentExtractJob job = getJob(extractJobId);

        if (!patentExtractStorageService.exists(job.getObjectKey())) {
            throw new PatentExtractException(ErrorCode.PATENT_DOCUMENT_NOT_FOUND);
        }

        job.markUploadCompleted(null);
        publishPatentExtractMessage(job);

        return PatentExtractJobStatusResponse.from(job);
    }

    public PatentExtractJobStatusResponse getStatus(Long extractJobId) {
        return PatentExtractJobStatusResponse.from(getJob(extractJobId));
    }

    public PatentExtractResultResponse getResult(Long extractJobId) {
        PatentExtractJob job = getJob(extractJobId);

        if (!job.isCompleted()) {
            throw new PatentExtractException(ErrorCode.PATENT_EXTRACT_NOT_COMPLETED);
        }

        return PatentExtractResultResponse.from(job);
    }

    private PatentExtractJob getJob(Long extractJobId) {
        return patentExtractJobRepository.findById(extractJobId)
                .orElseThrow(() -> new PatentExtractException(ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND));
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

    private void publishPatentExtractMessage(PatentExtractJob job) {
        try {
            patentExtractPublisher.publish(job.getId(), job.getObjectKey());
        } catch (RuntimeException e) {
            throw new PatentExtractException(ErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
