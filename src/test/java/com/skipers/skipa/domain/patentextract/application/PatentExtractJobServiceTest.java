package com.skipers.skipa.domain.patentextract.application;

import com.skipers.skipa.domain.patentextract.dao.PatentExtractJobRepository;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJobStatus;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractJobStatusResponse;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractUploadUrlResponse;
import com.skipers.skipa.domain.patentextract.exception.PatentExtractException;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
        when(patentExtractStorageService.generateUploadPresignedUrl("patents/extract-jobs/1/patent.pdf"))
                .thenReturn("https://minio.example.com/skipa/patents/extract-jobs/1/patent.pdf?signature=abc");

        PatentExtractUploadUrlResponse response = patentExtractJobService.createUploadUrl();

        assertThat(response.extractJobId()).isEqualTo(1L);
        assertThat(response.objectKey()).isEqualTo("patents/extract-jobs/1/patent.pdf");
        assertThat(response.uploadUrl()).isEqualTo("https://minio.example.com/skipa/patents/extract-jobs/1/patent.pdf?signature=abc");
        assertThat(response.expiresInSeconds()).isEqualTo(600);
        assertThat(response.status()).isEqualTo(PatentExtractJobStatus.UPLOAD_PENDING.name());
        verify(patentExtractStorageService).generateUploadPresignedUrl("patents/extract-jobs/1/patent.pdf");
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
                .generateUploadPresignedUrl("patents/extract-jobs/1/patent.pdf");

        assertThatThrownBy(() -> patentExtractJobService.createUploadUrl())
                .isInstanceOfSatisfying(PatentExtractException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR));
    }

    @Test
    void completeUploadVerifiesObjectAndPublishesMessage() {
        PatentExtractJob job = uploadPendingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(patentExtractStorageService.exists("patents/extract-jobs/1/patent.pdf")).thenReturn(true);

        PatentExtractJobStatusResponse response = patentExtractJobService.completeUpload(1L);

        assertThat(response.extractJobId()).isEqualTo(1L);
        assertThat(response.objectKey()).isEqualTo("patents/extract-jobs/1/patent.pdf");
        assertThat(response.status()).isEqualTo(PatentExtractJobStatus.ANALYZING.name());
        assertThat(response.uploadedAt()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.ANALYZING);
        verify(patentExtractPublisher).publish(1L, "patents/extract-jobs/1/patent.pdf");
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
        when(patentExtractStorageService.exists("patents/extract-jobs/1/patent.pdf")).thenReturn(false);

        assertPatentExtractError(() -> patentExtractJobService.completeUpload(1L), ErrorCode.PATENT_DOCUMENT_NOT_FOUND);

        assertThat(job.getStatus()).isEqualTo(PatentExtractJobStatus.UPLOAD_PENDING);
        verify(patentExtractPublisher, never()).publish(any(), any());
    }

    @Test
    void completeUploadWrapsPublisherFailure() {
        PatentExtractJob job = uploadPendingJob(1L);
        when(patentExtractJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(patentExtractStorageService.exists("patents/extract-jobs/1/patent.pdf")).thenReturn(true);
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(patentExtractPublisher)
                .publish(1L, "patents/extract-jobs/1/patent.pdf");

        assertPatentExtractError(() -> patentExtractJobService.completeUpload(1L), ErrorCode.EXTERNAL_SERVICE_ERROR);
    }

    private PatentExtractJob uploadPendingJob(Long extractJobId) {
        PatentExtractJob job = PatentExtractJob.createUploadPending();
        ReflectionTestUtils.setField(job, "id", extractJobId);
        job.assignObjectKey("patents/extract-jobs/%d/patent.pdf".formatted(extractJobId));
        return job;
    }

    private void assertPatentExtractError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(PatentExtractException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
