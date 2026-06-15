package com.skipers.skipa.domain.patentextract.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skipers.skipa.domain.patentextract.dao.PatentExtractJobRepository;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJobStatus;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractJobStatusResponse;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractResultResponse;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractUploadUrlResponse;
import com.skipers.skipa.domain.patentextract.exception.PatentExtractException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatentExtractJobServiceTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PatentExtractJobRepository patentExtractJobRepository;

    @Mock
    private PatentExtractStorageService patentExtractStorageService;

    @Mock
    private PatentExtractPublisher patentExtractPublisher;

    @InjectMocks
    private PatentExtractJobService patentExtractJobService;

    @Test
    void createUploadUrlSavesJobAndReturnsPresignedUrl() {
        ReflectionTestUtils.setField(patentExtractJobService, "presignedUrlExpirySeconds", 600);
        when(patentExtractJobRepository.save(any(PatentExtractJob.class))).thenAnswer(invocation -> {
            PatentExtractJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", 1L);
            return job;
        });
        when(patentExtractStorageService.generateUploadPresignedUrl("tmp/patent-extract-jobs/1/original.pdf"))
                .thenReturn("https://minio.example.com/skipa/tmp/patent-extract-jobs/1/original.pdf?signature=abc");

        PatentExtractUploadUrlResponse response = patentExtractJobService.createUploadUrl();

        assertThat(response.extractJobId()).isEqualTo(1L);
        assertThat(response.objectKey()).isEqualTo("tmp/patent-extract-jobs/1/original.pdf");
        assertThat(response.uploadUrl()).isEqualTo("https://minio.example.com/skipa/tmp/patent-extract-jobs/1/original.pdf?signature=abc");
        assertThat(response.expiresInSeconds()).isEqualTo(600);
        assertThat(response.status()).isEqualTo(PatentExtractJobStatus.UPLOAD_PENDING.name());
        verify(patentExtractStorageService).generateUploadPresignedUrl("tmp/patent-extract-jobs/1/original.pdf");
    }

    @Test
    void createUploadUrlWrapsStorageFailure() {
        ReflectionTestUtils.setField(patentExtractJobService, "presignedUrlExpirySeconds", 600);
        when(patentExtractJobRepository.save(any(PatentExtractJob.class))).thenAnswer(invocation -> {
            PatentExtractJob job = invocation.getArgument(0);
            ReflectionTestUtils.setField(job, "id", 1L);
            return job;
        });
        doThrow(new RuntimeException("MinIO unavailable"))
                .when(patentExtractStorageService)
                .generateUploadPresignedUrl("tmp/patent-extract-jobs/1/original.pdf");

        assertThatThrownBy(() -> patentExtractJobService.createUploadUrl())
                .isInstanceOfSatisfying(PatentExtractException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR));
    }

    @Test
    void completeUploadVerifiesObjectAndPublishesMessage() {
        PatentExtractJob job = uploadPendingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(patentExtractStorageService.exists("tmp/patent-extract-jobs/1/original.pdf")).thenReturn(true);

        PatentExtractJobStatusResponse response = patentExtractJobService.completeUpload(1L);

        assertThat(response.extractJobId()).isEqualTo(1L);
        assertThat(response.objectKey()).isEqualTo("tmp/patent-extract-jobs/1/original.pdf");
        assertThat(response.status()).isEqualTo(PatentExtractJobStatus.ANALYZING.name());
        assertThat(response.uploadedAt()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.ANALYZING);
        verify(patentExtractPublisher).publish(1L, "tmp/patent-extract-jobs/1/original.pdf");
    }

    @Test
    void completeUploadRejectsMissingJob() {
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentExtractError(() -> patentExtractJobService.completeUpload(1L), ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND);

        verify(patentExtractStorageService, never()).exists(any());
        verify(patentExtractPublisher, never()).publish(any(), any());
    }

    @Test
    void completeUploadRejectsMissingPdfWithoutPublishing() {
        PatentExtractJob job = uploadPendingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(patentExtractStorageService.exists("tmp/patent-extract-jobs/1/original.pdf")).thenReturn(false);

        assertPatentExtractError(() -> patentExtractJobService.completeUpload(1L), ErrorCode.PATENT_DOCUMENT_NOT_FOUND);

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.UPLOAD_PENDING);
        verify(patentExtractPublisher, never()).publish(any(), any());
    }

    @Test
    void completeUploadWrapsPublisherFailure() {
        PatentExtractJob job = uploadPendingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(patentExtractStorageService.exists("tmp/patent-extract-jobs/1/original.pdf")).thenReturn(true);
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(patentExtractPublisher)
                .publish(1L, "tmp/patent-extract-jobs/1/original.pdf");

        assertPatentExtractError(() -> patentExtractJobService.completeUpload(1L), ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    @Test
    void getStatusReturnsCurrentJobStatus() {
        PatentExtractJob job = uploadPendingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        PatentExtractJobStatusResponse response = patentExtractJobService.getStatus(1L);

        assertThat(response.extractJobId()).isEqualTo(1L);
        assertThat(response.objectKey()).isEqualTo("tmp/patent-extract-jobs/1/original.pdf");
        assertThat(response.status()).isEqualTo(PatentExtractJobStatus.UPLOAD_PENDING.name());
    }

    @Test
    void getStatusRejectsMissingJob() {
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentExtractError(() -> patentExtractJobService.getStatus(1L), ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND);
    }

    @Test
    void getResultReturnsCompletedResultJson() {
        PatentExtractJob job = completedJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        PatentExtractResultResponse response = patentExtractJobService.getResult(1L);

        assertThat(response.extractJobId()).isEqualTo(1L);
        assertThat(response.objectKey()).isEqualTo("tmp/patent-extract-jobs/1/original.pdf");
        assertThat(response.status()).isEqualTo(PatentExtractJobStatus.COMPLETED.name());
        assertThat(response.result()).isInstanceOfSatisfying(java.util.Map.class,
                result -> assertThat(result.get("title")).isEqualTo("Patent"));
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void getResultRejectsJobThatIsNotCompleted() {
        PatentExtractJob job = uploadPendingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertPatentExtractError(() -> patentExtractJobService.getResult(1L), ErrorCode.PATENT_EXTRACT_NOT_COMPLETED);
    }

    @Test
    void getResultRejectsMissingJob() {
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentExtractError(() -> patentExtractJobService.getResult(1L), ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND);
    }

    @Test
    void completeStoresResultAndMarksJobCompleted() {
        PatentExtractJob job = analyzingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        PatentExtractJobStatusResponse response = patentExtractJobService.complete(
                1L,
                objectMapper.createObjectNode()
                        .put("title", "Patent")
                        .put("summary", "Summary"),
                "tmp/patent-extract-jobs/1/parsed.json"
        );

        assertThat(response.extractJobId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(PatentExtractJobStatus.COMPLETED.name());
        assertThat(response.parsedJsonKey()).isEqualTo("tmp/patent-extract-jobs/1/parsed.json");
        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.COMPLETED);
        assertThat(job.getResultJson().get("title").asText()).isEqualTo("Patent");
        assertThat(job.getParsedJsonKey()).isEqualTo("tmp/patent-extract-jobs/1/parsed.json");
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void completeRejectsMissingJob() {
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentExtractError(
                () -> patentExtractJobService.complete(
                        1L,
                        objectMapper.createObjectNode().put("title", "Patent"),
                        "tmp/patent-extract-jobs/1/parsed.json"
                ),
                ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND
        );
    }

    @Test
    void completeRejectsNullResult() {
        PatentExtractJob job = analyzingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertPatentExtractError(
                () -> patentExtractJobService.complete(1L, null, "tmp/patent-extract-jobs/1/parsed.json"),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.ANALYZING);
    }

    @Test
    void completeRejectsBlankParsedJsonKey() {
        PatentExtractJob job = analyzingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertPatentExtractError(
                () -> patentExtractJobService.complete(1L, objectMapper.createObjectNode().put("title", "Patent"), " "),
                ErrorCode.INVALID_REQUEST
        );

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.ANALYZING);
    }

    @Test
    void completeRejectsJobThatIsNotAnalyzing() {
        PatentExtractJob job = uploadPendingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertPatentExtractError(
                () -> patentExtractJobService.complete(
                        1L,
                        objectMapper.createObjectNode().put("title", "Patent"),
                        "tmp/patent-extract-jobs/1/parsed.json"
                ),
                ErrorCode.PATENT_EXTRACT_ALREADY_PROCESSED
        );
    }

    @Test
    void failStoresErrorMessageAndMarksJobFailed() {
        PatentExtractJob job = analyzingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        PatentExtractJobStatusResponse response = patentExtractJobService.fail(1L, "AI extraction failed");

        assertThat(response.extractJobId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(PatentExtractJobStatus.FAILED.name());
        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("AI extraction failed");
        assertThat(job.getCompletedAt()).isNotNull();
    }

    @Test
    void failRejectsMissingJob() {
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentExtractError(() -> patentExtractJobService.fail(1L, "AI extraction failed"), ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND);
    }

    @Test
    void failRejectsAlreadyCompletedJob() {
        PatentExtractJob job = completedJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertPatentExtractError(() -> patentExtractJobService.fail(1L, "retry failed"), ErrorCode.PATENT_EXTRACT_ALREADY_PROCESSED);

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.COMPLETED);
    }

    private PatentExtractJob uploadPendingJob(Long extractJobId) {
        PatentExtractJob job = PatentExtractJob.createUploadPending();
        ReflectionTestUtils.setField(job, "id", extractJobId);
        job.assignObjectKey("tmp/patent-extract-jobs/%d/original.pdf".formatted(extractJobId));
        return job;
    }

    private PatentExtractJob completedJob(Long extractJobId) {
        PatentExtractJob job = analyzingJob(extractJobId);
        job.complete(
                objectMapper.createObjectNode().put("title", "Patent"),
                "tmp/patent-extract-jobs/%d/parsed.json".formatted(extractJobId),
                null
        );
        return job;
    }

    private PatentExtractJob analyzingJob(Long extractJobId) {
        PatentExtractJob job = uploadPendingJob(extractJobId);
        job.markUploadCompleted(null);
        return job;
    }

    private void assertPatentExtractError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PatentExtractException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
