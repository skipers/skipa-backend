package com.skipers.skipa.domain.review.api;

import com.skipers.skipa.domain.review.application.ReviewService;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('LEGAL')")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 특허를 담당 부서에 검토 요청한다.
     *
     * @param patentId 특허 ID
     * @return 생성된 사업부 검토
     */
    @Operation(summary = "사업부 검토 요청", description = "Legal 팀이 특허를 담당 부서에 검토 요청합니다.")
    @PostMapping("/patents/{patentId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(@PathVariable Long patentId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reviewService.create(patentId)));
    }

    /**
     * 사업부 검토 목록을 조회한다(page/size 기반).
     *
     * @param status 제출 상태(선택)
     * @param departmentId 부서 ID(선택)
     * @param patentId 특허 ID(선택)
     * @param pageable page/size 정보
     * @return 사업부 검토 목록 페이지
     */
    @Operation(summary = "사업부 검토 목록 조회", description = "Legal 팀이 사업부 검토 목록을 조회합니다. 제출 상태, 부서, 특허로 필터링할 수 있습니다.")
    @GetMapping("/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long patentId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(reviewService.getAll(status, departmentId, patentId, pageable)));
    }

    /**
     * 사업부 검토를 ID로 조회한다.
     *
     * @param reviewId 사업부 검토 ID
     * @return 사업부 검토 상세 정보
     */
    @Operation(summary = "사업부 검토 단일 조회", description = "Legal 팀이 검토 요청과 사업부의 의견 제출 정보를 조회합니다.")
    @GetMapping("/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> get(@PathVariable Long reviewId) {
        return ApiResponse.ok(reviewService.get(reviewId));
    }
}
