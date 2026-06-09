package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.BusinessReviewService;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import com.skipers.skipa.domain.review.dto.request.ReviewSubmitRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/assigned-patents")
public class BusinessReviewController {

    private final BusinessReviewService businessReviewService;

    /**
     * 본인 소속 부서에 요청된 특허 검토 현황 목록을 조회한다(page/size 기반).
     *
     * @param userDetails 인증 사용자 정보
     * @param status 제출 상태(선택)
     * @param opinion 사업부 의견(선택)
     * @param submittedFrom 제출일 시작일(선택)
     * @param submittedTo 제출일 종료일(선택)
     * @param pageable page/size 정보
     * @return 사업부 검토 현황 목록 페이지
     */
    @Operation(
            summary = "[Business] 사업부 검토 현황 목록 조회",
            description = "사업부 사용자의 소속 부서에 요청된 특허 검토 현황 목록을 조회합니다. "
                    + "필터: status(PENDING, SUBMITTED), opinion(MAINTAIN, ABANDON), "
                    + "submittedFrom/submittedTo(제출일, YYYY-MM-DD, 양 끝 포함). "
                    + "정렬: 최신 검토 요청순(id 내림차순) 고정입니다."
    )
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<BusinessReviewResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String opinion,
            @RequestParam(required = false) LocalDate submittedFrom,
            @RequestParam(required = false) LocalDate submittedTo,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(businessReviewService.getAll(
                userDetails.getUser(),
                status,
                opinion,
                submittedFrom,
                submittedTo,
                pageable
        )));
    }

    /**
     * 본인 소속 부서에 요청된 특허 검토 현황을 ID로 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @return 사업부 검토 현황 상세 정보
     */
    @Operation(summary = "[Business] 사업부 검토 현황 단일 조회", description = "사업부 사용자의 소속 부서에 요청된 특허 검토 현황과 의견 제출 정보를 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/{patentId}")
    public ApiResponse<BusinessReviewDetailResponse> get(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId
    ) {
        return ApiResponse.ok(businessReviewService.get(userDetails.getUser(), patentId));
    }

    /**
     * 본인 소속 부서에 요청된 특허 검토에 의견을 제출한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @param request 의견 제출 요청
     * @return 의견이 반영된 사업부 검토 현황
     */
    @Operation(summary = "[Business] 사업부 검토 의견 제출", description = "사업부 사용자가 요청된 특허 검토에 MAINTAIN 또는 ABANDON 의견을 제출합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @PostMapping("/{patentId}/opinions")
    public ApiResponse<BusinessReviewResponse> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @Valid @RequestBody ReviewSubmitRequest request
    ) {
        return ApiResponse.ok(businessReviewService.submit(userDetails.getUser(), patentId, request));
    }
}
