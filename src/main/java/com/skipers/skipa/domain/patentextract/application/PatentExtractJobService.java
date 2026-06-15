package com.skipers.skipa.domain.patentextract.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

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

        return PatentExtractResultResponse.of(job, toResponseResult(job.getResultJson()));
    }

    @Transactional
    public PatentExtractJobStatusResponse complete(Long extractJobId, JsonNode result, String parsedJsonKey) {
        PatentExtractJob job = getJob(extractJobId);

        job.complete(result, parsedJsonKey, null);

        return PatentExtractJobStatusResponse.from(job);
    }

    @Transactional
    public PatentExtractJobStatusResponse fail(Long extractJobId, String errorMessage) {
        PatentExtractJob job = getJob(extractJobId);

        job.fail(errorMessage);

        return PatentExtractJobStatusResponse.from(job);
    }

    private PatentExtractJob getJob(Long extractJobId) {
        return patentExtractJobRepository.findById(extractJobId)
                .orElseThrow(() -> new PatentExtractException(ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND));
    }

    private JsonNode normalizeResultJson(JsonNode resultJson) {
        if (resultJson == null || !resultJson.isTextual()) {
            return resultJson;
        }

        try {
            return objectMapper.readTree(resultJson.asText());
        } catch (Exception e) {
            return resultJson;
        }
    }

    private Object toResponseResult(JsonNode resultJson) {
        JsonNode normalizedResultJson = normalizeResultJson(resultJson);
        if (normalizedResultJson == null || normalizedResultJson.isNull()) {
            return null;
        }

        return objectMapper.convertValue(normalizedResultJson, Object.class);
    }

    private String buildTemporaryPdfObjectKey(Long extractJobId) {
        return "tmp/patent-extract-jobs/%d/original.pdf".formatted(extractJobId);
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
