package com.skipers.skipa.domain.patentextract.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.patentextract.exception.PatentExtractException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatentExtractJobTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createUploadPendingInitializesStatus() {
        PatentExtractJob job = PatentExtractJob.createUploadPending();

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.UPLOAD_PENDING);
        assertThat(job.getObjectKey()).isNull();
        assertThat(job.isCompleted()).isFalse();
    }

    @Test
    void assignObjectKeyStoresTemporaryPdfKey() {
        PatentExtractJob job = PatentExtractJob.createUploadPending();

        job.assignObjectKey("tmp/patent-extract-jobs/1/original.pdf");

        assertThat(job.getObjectKey()).isEqualTo("tmp/patent-extract-jobs/1/original.pdf");
        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.UPLOAD_PENDING);
    }

    @Test
    void assignObjectKeyRejectsBlankKey() {
        PatentExtractJob job = PatentExtractJob.createUploadPending();

        assertPatentExtractError(() -> job.assignObjectKey(" "), ErrorCode.INVALID_REQUEST);
    }

    @Test
    void markUploadCompletedChangesStatusToAnalyzing() {
        PatentExtractJob job = uploadPendingJob();
        Instant uploadedAt = Instant.parse("2026-06-08T01:00:00Z");

        job.markUploadCompleted(uploadedAt);

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.ANALYZING);
        assertThat(job.getUploadedAt()).isEqualTo(uploadedAt);
    }

    @Test
    void markUploadCompletedRequiresObjectKey() {
        PatentExtractJob job = PatentExtractJob.createUploadPending();

        assertPatentExtractError(
                () -> job.markUploadCompleted(Instant.parse("2026-06-08T01:00:00Z")),
                ErrorCode.INVALID_REQUEST
        );
    }

    @Test
    void completeStoresResultAndCompletionTime() {
        PatentExtractJob job = analyzingJob();
        JsonNode resultJson = objectMapper.createObjectNode()
                .put("title", "Patent")
                .put("summary", "Summary");
        Instant completedAt = Instant.parse("2026-06-08T01:10:00Z");

        job.complete(resultJson, "tmp/patent-extract-jobs/1/parsed.json", completedAt);

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.COMPLETED);
        assertThat(job.getResultJson()).isEqualTo(resultJson);
        assertThat(job.getParsedJsonKey()).isEqualTo("tmp/patent-extract-jobs/1/parsed.json");
        assertThat(job.getCompletedAt()).isEqualTo(completedAt);
        assertThat(job.isCompleted()).isTrue();
    }

    @Test
    void completeRejectsNullResult() {
        PatentExtractJob job = analyzingJob();

        assertPatentExtractError(
                () -> job.complete(null, "tmp/patent-extract-jobs/1/parsed.json", Instant.parse("2026-06-08T01:10:00Z")),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.ANALYZING);
    }

    @Test
    void completeRejectsBlankParsedJsonKey() {
        PatentExtractJob job = analyzingJob();

        assertPatentExtractError(
                () -> job.complete(objectMapper.createObjectNode().put("title", "Patent"), " ", Instant.parse("2026-06-08T01:10:00Z")),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.ANALYZING);
    }

    @Test
    void failStoresErrorMessageAndCompletionTime() {
        PatentExtractJob job = analyzingJob();

        job.fail("AI worker failed");

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("AI worker failed");
        assertThat(job.getCompletedAt()).isNotNull();
        assertThat(job.isCompleted()).isFalse();
    }

    @Test
    void completedJobCannotBeFailedAgain() {
        PatentExtractJob job = analyzingJob();
        job.complete(
                objectMapper.createObjectNode().put("title", "Patent"),
                "tmp/patent-extract-jobs/1/parsed.json",
                Instant.parse("2026-06-08T01:10:00Z")
        );

        assertPatentExtractError(() -> job.fail("retry failed"), ErrorCode.PATENT_EXTRACT_ALREADY_PROCESSED);

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.COMPLETED);
    }

    @Test
    void failedJobCannotBeCompletedAgain() {
        PatentExtractJob job = analyzingJob();
        job.fail("AI worker failed");

        assertPatentExtractError(
                () -> job.complete(
                        objectMapper.createObjectNode().put("title", "Patent"),
                        "tmp/patent-extract-jobs/1/parsed.json",
                        Instant.parse("2026-06-08T01:10:00Z")
                ),
                ErrorCode.PATENT_EXTRACT_ALREADY_PROCESSED
        );

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.FAILED);
    }

    private PatentExtractJob uploadPendingJob() {
        PatentExtractJob job = PatentExtractJob.createUploadPending();
        job.assignObjectKey("tmp/patent-extract-jobs/1/original.pdf");
        return job;
    }

    private PatentExtractJob analyzingJob() {
        PatentExtractJob job = uploadPendingJob();
        job.markUploadCompleted(Instant.parse("2026-06-08T01:00:00Z"));
        return job;
    }

    private void assertPatentExtractError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PatentExtractException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
