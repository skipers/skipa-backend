package com.skipers.skipa.domain.patentextract.api;

import com.skipers.skipa.domain.patentextract.application.PatentExtractJobService;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractJobStatusResponse;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractResultResponse;
import com.skipers.skipa.domain.patentextract.dto.response.PatentExtractUploadUrlResponse;
import com.skipers.skipa.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patent-extract-jobs")
public class PatentExtractJobController {

    private final PatentExtractJobService patentExtractJobService;

    @Operation(summary = "[Common] 특허 원문 PDF 업로드 URL 발급", description = "특허 추출 작업을 생성하고 PDF 업로드용 presigned URL을 발급합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<PatentExtractUploadUrlResponse>> createUploadUrl() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(patentExtractJobService.createUploadUrl()));
    }

    @Operation(summary = "[Common] 특허 원문 PDF 업로드 완료", description = "업로드된 PDF 존재 여부를 확인하고 특허 추출 작업을 AI Worker 큐에 발행합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @PostMapping("/{extractJobId}/upload-complete")
    public ApiResponse<PatentExtractJobStatusResponse> completeUpload(@PathVariable Long extractJobId) {
        return ApiResponse.ok(patentExtractJobService.completeUpload(extractJobId));
    }

    @Operation(summary = "[Common] 특허 추출 작업 상태 조회", description = "특허 추출 작업 상태를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/{extractJobId}/status")
    public ApiResponse<PatentExtractJobStatusResponse> getStatus(@PathVariable Long extractJobId) {
        return ApiResponse.ok(patentExtractJobService.getStatus(extractJobId));
    }

    @Operation(summary = "[Common] 특허 추출 결과 조회", description = "완료된 특허 추출 작업의 결과 JSON을 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/{extractJobId}/result")
    public ApiResponse<PatentExtractResultResponse> getResult(@PathVariable Long extractJobId) {
        return ApiResponse.ok(patentExtractJobService.getResult(extractJobId));
    }
}
