package com.skipers.skipa.domain.preevaluation.api;

import com.skipers.skipa.domain.preevaluation.application.PreEvaluationService;
import com.skipers.skipa.domain.preevaluation.dto.request.PreEvaluationCreateRequest;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationCreateResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationDetailResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationResponse;
import com.skipers.skipa.domain.preevaluation.dto.response.PreEvaluationStatusResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pre-evaluations")
public class PreEvaluationController {

    private final PreEvaluationService preEvaluationService;

    @Operation(summary = "[Common] 사전 평가 시작", description = "임시 특허 정보를 저장하고 사전 평가 보고서 생성을 요청합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @PostMapping
    public ResponseEntity<ApiResponse<PreEvaluationCreateResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PreEvaluationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(preEvaluationService.create(userDetails.getUser(), request)));
    }

    @Operation(summary = "[Common] 사전 평가 이력 목록 조회", description = "현재 사용자의 사전 평가 이력 목록을 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<PreEvaluationResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(page = 0, size = 50) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(preEvaluationService.getAll(userDetails.getUser(), pageable)));
    }

    @Operation(summary = "[Common] 사전 평가 상세 조회", description = "현재 사용자의 사전 평가 상세 정보를 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/{preEvaluationId}")
    public ApiResponse<PreEvaluationDetailResponse> get(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preEvaluationId
    ) {
        return ApiResponse.ok(preEvaluationService.get(userDetails.getUser(), preEvaluationId));
    }

    @Operation(summary = "[Common] 사전 평가 처리 상태 조회", description = "현재 사용자의 사전 평가 보고서 생성 및 임베딩 처리 상태를 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/{preEvaluationId}/status")
    public ApiResponse<PreEvaluationStatusResponse> getStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preEvaluationId
    ) {
        return ApiResponse.ok(preEvaluationService.getStatus(userDetails.getUser(), preEvaluationId));
    }

    @Operation(summary = "[Common] 사전 평가 이력 삭제", description = "현재 사용자의 사전 평가 이력과 채팅 메시지를 삭제합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @DeleteMapping("/{preEvaluationId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long preEvaluationId
    ) {
        preEvaluationService.delete(userDetails.getUser(), preEvaluationId);
        return ApiResponse.ok(null);
    }
}
