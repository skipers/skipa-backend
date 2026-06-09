package com.skipers.skipa.domain.review.api;

import com.skipers.skipa.domain.review.application.ReviewCycleService;
import com.skipers.skipa.domain.review.dto.request.ReviewCycleCreateRequest;
import com.skipers.skipa.domain.review.dto.request.ReviewCycleUpdateRequest;
import com.skipers.skipa.domain.review.dto.response.ReviewCycleResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/review-cycles")
public class ReviewCycleController {

    private final ReviewCycleService reviewCycleService;

    /**
     * 검토 주기를 생성한다.
     *
     * @param request 생성 요청
     * @return 생성된 검토 주기
     */
    @Operation(summary = "[Admin] 검토 주기 생성", description = "Admin이 사업부 검토 요청에 사용할 검토 주기를 생성합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewCycleResponse>> create(
            @Valid @RequestBody ReviewCycleCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reviewCycleService.create(request)));
    }

    /**
     * 현재 활성화된 검토 주기를 조회한다.
     *
     * @return 현재 검토 주기
     */
    @Operation(summary = "[Common] 현재 검토 주기 조회", description = "오늘 날짜가 포함된 현재 활성 검토 주기의 연도, 분기, 기간 범위를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/current")
    public ApiResponse<ReviewCycleResponse> getCurrent() {
        return ApiResponse.ok(reviewCycleService.getCurrent());
    }

    /**
     * 검토 주기를 ID로 조회한다.
     *
     * @param reviewCycleId 검토 주기 ID
     * @return 검토 주기
     */
    @Operation(summary = "[Common] 검토 주기 단일 조회", description = "관리자, Legal 팀, Business 팀이 검토 주기 ID로 상세 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/{reviewCycleId}")
    public ApiResponse<ReviewCycleResponse> get(@PathVariable Long reviewCycleId) {
        return ApiResponse.ok(reviewCycleService.get(reviewCycleId));
    }

    /**
     * 검토 주기 목록을 조회한다(page/size 기반).
     *
     * @param pageable page/size 정보
     * @return 검토 주기 목록 페이지
     */
    @Operation(
            summary = "[Common] 검토 주기 목록 조회",
            description = "관리자, Legal 팀, Business 팀이 검토 주기 목록을 페이지 단위로 조회합니다. "
                    + "필터는 제공하지 않습니다. "
                    + "정렬: 시작일 내림차순(startDate DESC) 고정입니다."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<ReviewCycleResponse>> getAll(
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(reviewCycleService.getAll(pageable)));
    }

    /**
     * 검토 주기를 수정한다.
     *
     * @param reviewCycleId 검토 주기 ID
     * @param request 수정 요청
     * @return 수정된 검토 주기
     */
    @Operation(summary = "[Admin] 검토 주기 수정", description = "Admin이 검토 주기 정보를 수정합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{reviewCycleId}")
    public ApiResponse<ReviewCycleResponse> update(
            @PathVariable Long reviewCycleId,
            @Valid @RequestBody ReviewCycleUpdateRequest request
    ) {
        return ApiResponse.ok(reviewCycleService.update(reviewCycleId, request));
    }

    /**
     * 검토 주기를 삭제한다.
     *
     * @param reviewCycleId 검토 주기 ID
     * @return 성공 응답
     */
    @Operation(summary = "[Admin] 검토 주기 삭제", description = "Admin이 검토 주기를 삭제합니다. 검토 요청에서 사용 중인 주기는 삭제할 수 없습니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{reviewCycleId}")
    public ApiResponse<Void> delete(@PathVariable Long reviewCycleId) {
        reviewCycleService.delete(reviewCycleId);
        return ApiResponse.ok();
    }
}
