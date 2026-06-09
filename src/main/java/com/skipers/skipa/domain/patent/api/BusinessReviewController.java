package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.BusinessReviewService;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewHistoryResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewResponse;
import com.skipers.skipa.domain.patent.dto.response.BusinessReviewSummaryResponse;
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
@RequestMapping("/business-reviews")
public class BusinessReviewController {

    private final BusinessReviewService businessReviewService;

    /**
     * 본인 소속 부서의 현재 검토 현황 요약을 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @return 현재 검토 주기와 제출 현황 KPI
     */
    @Operation(
            summary = "[Business] 사업부 검토 현황 요약 조회",
            description = "사업부 사용자의 소속 부서 기준 현재 검토 주기 정보와 제출 완료/미제출 KPI를 조회합니다."
    )
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/summary")
    public ApiResponse<BusinessReviewSummaryResponse> getSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.ok(businessReviewService.getSummary(userDetails.getUser()));
    }

    /**
     * 본인 소속 부서에 현재 검토 주기로 요청된 특허 검토 현황 목록을 조회한다(page/size 기반).
     *
     * @param userDetails 인증 사용자 정보
     * @param status 제출 상태(선택)
     * @param opinion 사업부 의견(선택)
     * @param submittedFrom 제출일 시작일(선택)
     * @param submittedTo 제출일 종료일(선택)
     * @param sort 정렬 기준(선택)
     * @param pageable page/size 정보
     * @return 사업부 검토 현황 목록 페이지
     */
    @Operation(
            summary = "[Business] 사업부 검토 현황 목록 조회",
            description = "사업부 사용자의 소속 부서에 현재 검토 주기로 요청된 특허 검토 현황 목록을 조회합니다. "
                    + "각 특허의 요약, 키워드, 최신 완료 평가 보고서 점수와 등급을 함께 반환합니다. "
                    + "필터: status(PENDING, OVERDUE, SUBMITTED), opinion(MAINTAIN, ABANDON), "
                    + "submittedFrom/submittedTo(제출일, YYYY-MM-DD, 양 끝 포함). "
                    + "정렬: sort=title,asc|desc, applicationNumber,asc|desc, applicationDate,asc|desc, expiryDate,asc|desc. "
                    + "미지정 시 출원번호 오름차순(applicationNumber ASC)입니다."
    )
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<BusinessReviewResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String opinion,
            @RequestParam(required = false) LocalDate submittedFrom,
            @RequestParam(required = false) LocalDate submittedTo,
            @RequestParam(required = false) String sort,
            @PageableDefault(page = 0, size = 50) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(businessReviewService.getAll(
                userDetails.getUser(),
                status,
                opinion,
                submittedFrom,
                submittedTo,
                sort,
                pageable
        )));
    }

    /**
     * 본인 소속 부서의 과거 제출 이력을 조회한다(page/size 기반).
     *
     * @param userDetails 인증 사용자 정보
     * @param year 검토 주기 연도(선택)
     * @param quarter 검토 주기 분기(선택, year와 함께 사용)
     * @param opinion 사업부 의견(선택)
     * @param pageable page/size 정보
     * @return 과거 사업부 검토 제출 이력 목록 페이지
     */
    @Operation(
            summary = "[Business] 사업부 검토 과거 제출 이력 조회",
            description = "사업부 사용자의 소속 부서 기준 과거 검토 주기의 제출 완료 이력을 조회합니다. "
                    + "필터: 미지정 시 전체, year 지정 시 해당 연도 전체, year와 quarter 지정 시 해당 연도/분기, "
                    + "opinion(MAINTAIN, ABANDON) 지정 시 유지/포기 의견입니다. "
                    + "현재 진행 중인 검토 주기는 포함하지 않습니다."
    )
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/history")
    public ApiResponse<PageResponse<BusinessReviewHistoryResponse>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String opinion,
            @PageableDefault(page = 0, size = 50) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(businessReviewService.getHistory(
                userDetails.getUser(),
                year,
                quarter,
                opinion,
                pageable
        )));
    }

    /**
     * 본인 소속 부서에 현재 검토 주기로 요청된 특허 검토 현황을 ID로 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @return 사업부 검토 현황 상세 정보
     */
    @Operation(summary = "[Business] 사업부 검토 현황 단일 조회", description = "사업부 사용자의 소속 부서에 현재 검토 주기로 요청된 특허 검토 현황과 의견 제출 정보를 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/{patentId}")
    public ApiResponse<BusinessReviewDetailResponse> get(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId
    ) {
        return ApiResponse.ok(businessReviewService.get(userDetails.getUser(), patentId));
    }

    /**
     * 본인 소속 부서에 현재 검토 주기로 요청된 특허 검토에 의견을 제출한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @param request 의견 제출 요청
     * @return 의견이 반영된 사업부 검토 현황
     */
    @Operation(summary = "[Business] 사업부 검토 의견 제출", description = "사업부 사용자가 현재 검토 주기로 요청된 특허 검토에 MAINTAIN 또는 ABANDON 의견을 제출합니다.")
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
