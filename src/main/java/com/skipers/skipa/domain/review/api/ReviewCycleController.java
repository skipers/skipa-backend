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
    @Operation(summary = "검토 주기 생성", description = "Legal 팀이 사업부 검토 요청에 사용할 검토 주기를 생성합니다.")
    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewCycleResponse>> create(
            @Valid @RequestBody ReviewCycleCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reviewCycleService.create(request)));
    }

    /**
     * 검토 주기를 ID로 조회한다.
     *
     * @param reviewCycleId 검토 주기 ID
     * @return 검토 주기
     */
    @Operation(summary = "검토 주기 단일 조회", description = "관리자와 Legal 팀이 검토 주기 ID로 상세 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
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
    @Operation(summary = "검토 주기 목록 조회", description = "관리자와 Legal 팀이 검토 주기 목록을 최근 시작일 순으로 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
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
    @Operation(summary = "검토 주기 수정", description = "Legal 팀이 검토 주기 정보를 수정합니다.")
    @PreAuthorize("hasRole('LEGAL')")
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
    @Operation(summary = "검토 주기 삭제", description = "Legal 팀이 검토 주기를 삭제합니다. 검토 요청에서 사용 중인 주기는 삭제할 수 없습니다.")
    @PreAuthorize("hasRole('LEGAL')")
    @DeleteMapping("/{reviewCycleId}")
    public ApiResponse<Void> delete(@PathVariable Long reviewCycleId) {
        reviewCycleService.delete(reviewCycleId);
        return ApiResponse.ok();
    }
}
