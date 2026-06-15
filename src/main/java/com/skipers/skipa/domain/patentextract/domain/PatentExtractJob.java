package com.skipers.skipa.domain.patentextract.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.skipers.skipa.domain.patentextract.exception.PatentExtractException;
import com.skipers.skipa.global.common.entity.BaseTimeEntity;
import com.skipers.skipa.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "patent_extract_jobs")
public class PatentExtractJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "object_key", length = 500)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PatentExtractJobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "jsonb")
    private JsonNode resultJson;

    @Column(name = "parsed_json_key", length = 500)
    private String parsedJsonKey;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder
    private PatentExtractJob(
            String objectKey,
            PatentExtractJobStatus status,
            JsonNode resultJson,
            String parsedJsonKey,
            String errorMessage,
            Instant uploadedAt,
            Instant completedAt
    ) {
        this.objectKey = objectKey;
        this.status = status != null ? status : PatentExtractJobStatus.UPLOAD_PENDING;
        this.resultJson = resultJson;
        this.parsedJsonKey = parsedJsonKey;
        this.errorMessage = errorMessage;
        this.uploadedAt = uploadedAt;
        this.completedAt = completedAt;
    }

    public static PatentExtractJob createUploadPending() {
        return PatentExtractJob.builder()
                .status(PatentExtractJobStatus.UPLOAD_PENDING)
                .build();
    }

    public void assignObjectKey(String objectKey) {
        validateUploadPending();

        if (objectKey == null || objectKey.isBlank()) {
            throw new PatentExtractException(ErrorCode.INVALID_REQUEST);
        }

        this.objectKey = objectKey;
    }

    public void markUploadCompleted(Instant uploadedAt) {
        validateUploadPending();

        if (objectKey == null || objectKey.isBlank()) {
            throw new PatentExtractException(ErrorCode.INVALID_REQUEST);
        }

        this.status = PatentExtractJobStatus.ANALYZING;
        this.uploadedAt = uploadedAt != null ? uploadedAt : Instant.now();
    }

    public void complete(JsonNode resultJson, String parsedJsonKey, Instant completedAt) {
        validateAnalyzing();

        if (resultJson == null || resultJson.isNull()) {
            throw new PatentExtractException(ErrorCode.INVALID_REQUEST);
        }
        if (parsedJsonKey == null || parsedJsonKey.isBlank()) {
            throw new PatentExtractException(ErrorCode.INVALID_REQUEST);
        }

        this.resultJson = resultJson;
        this.parsedJsonKey = parsedJsonKey;
        this.status = PatentExtractJobStatus.COMPLETED;
        this.completedAt = completedAt != null ? completedAt : Instant.now();
    }

    public void fail(String errorMessage) {
        validateNotProcessed();

        this.status = PatentExtractJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    public boolean isCompleted() {
        return status == PatentExtractJobStatus.COMPLETED;
    }

    private void validateUploadPending() {
        if (status != PatentExtractJobStatus.UPLOAD_PENDING) {
            throw new PatentExtractException(ErrorCode.PATENT_EXTRACT_ALREADY_PROCESSED);
        }
    }

    private void validateAnalyzing() {
        if (status != PatentExtractJobStatus.ANALYZING) {
            throw new PatentExtractException(ErrorCode.PATENT_EXTRACT_ALREADY_PROCESSED);
        }
    }

    private void validateNotProcessed() {
        if (status == PatentExtractJobStatus.COMPLETED || status == PatentExtractJobStatus.FAILED) {
            throw new PatentExtractException(ErrorCode.PATENT_EXTRACT_ALREADY_PROCESSED);
        }
    }
}
