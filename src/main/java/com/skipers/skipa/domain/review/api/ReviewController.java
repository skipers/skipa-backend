package com.skipers.skipa.domain.review.api;

import com.skipers.skipa.domain.review.application.ReviewService;
import com.skipers.skipa.domain.review.dto.request.BulkReviewCreateRequest;
import com.skipers.skipa.domain.review.dto.response.BulkReviewCreateResponse;
import com.skipers.skipa.domain.review.dto.response.ReviewConfirmResponse;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 특허를 담당 부서에 검토 요청한다.
     *
     * @param patentId 특허 ID
     * @return 생성된 검토 요청
     */
    @Operation(summary = "[Legal] 검토 요청", description = "Legal 팀이 특정 특허를 담당 부서에 검토 요청합니다. 이미 이번 분기 검토 대상으로 예약된 특허라면 요청 상태로 전환합니다.")
    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping("/patents/{patentId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@PathVariable Long patentId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reviewService.create(patentId)));
    }

    /**
     * 여러 특허를 담당 부서에 검토 요청한다.
     *
     * @param request 특허 ID 목록
     * @return 생성 및 건너뜀 결과
     */
    @Operation(summary = "[Legal] 검토 일괄 요청", description = "Legal 팀이 여러 특허를 담당 부서에 한 번에 검토 요청합니다. 이미 이번 분기 검토 대상으로 예약된 특허는 요청 상태로 전환합니다.")
    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping("/reviews/bulk")
    public ResponseEntity<ApiResponse<BulkReviewCreateResponse>> createBulk(
            @Valid @RequestBody BulkReviewCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reviewService.createBulk(request)));
    }

    /**
     * 이번 분기 검토 대상 특허 목록을 조회한다(page/size 기반).
     *
     * @param status 제출 상태(선택)
     * @param departmentId 부서 ID(선택)
     * @param patentId 특허 ID(선택)
     * @param checked 회신 확인 여부(선택)
     * @param sort 정렬 기준(선택)
     * @param pageable page/size 정보
     * @return 이번 분기 검토 대상 특허 목록 페이지
     */
    @Operation(
            summary = "[Legal] 이번 분기 검토 대상 특허 목록 조회",
            description = "관리자와 Legal 팀이 현재 활성 검토 주기에 포함된 검토 대상 특허 목록을 조회합니다. "
                    + "필터: status(SCHEDULED, PENDING, OVERDUE, SUBMITTED), departmentId, patentId, checked(true/false). "
                    + "정렬: sort=title,asc|desc, applicationNumber,asc|desc, applicationDate,asc|desc, expiryDate,asc|desc. "
                    + "미지정 시 출원번호 오름차순(applicationNumber ASC)입니다."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/review-targets")
    public ApiResponse<PageResponse<ReviewResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long patentId,
            @RequestParam(required = false) Boolean checked,
            @RequestParam(required = false) String sort,
            @PageableDefault(page = 0, size = 50) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(reviewService.getAll(
                status,
                departmentId,
                patentId,
                checked,
                sort,
                pageable
        )));
    }

    /**
     * 검토 대상 특허를 ID로 조회한다.
     *
     * @param reviewId 검토 ID
     * @return 검토 대상 특허 상세 정보
     */
    @Operation(summary = "[Legal] 검토 대상 특허 단일 조회", description = "관리자와 Legal 팀이 검토 대상 특허의 요청 상태와 사업부 회신 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/review-targets/{reviewId}")
    public ApiResponse<ReviewResponse> get(@PathVariable Long reviewId) {
        return ApiResponse.ok(reviewService.get(reviewId));
    }

    /**
     * Legal 팀이 제출된 사업부 회신을 확인 처리한다.
     *
     * @param reviewId 검토 ID
     * @return 확인 처리 결과
     */
    @Operation(summary = "[Legal] 검토 회신 확인", description = "Legal 팀이 제출된 사업부 검토 회신을 확인 처리합니다.")
    @PreAuthorize("hasRole('LEGAL')")
    @PatchMapping("/reviews/{reviewId}/confirm")
    public ApiResponse<ReviewConfirmResponse> confirm(@PathVariable Long reviewId) {
        return ApiResponse.ok(reviewService.confirm(reviewId));
    }
}
