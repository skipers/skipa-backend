package com.skipers.skipa.domain.review.api;

import com.skipers.skipa.domain.review.application.ReviewService;
import com.skipers.skipa.domain.review.dto.request.ReviewCreateRequest;
import com.skipers.skipa.domain.review.dto.response.ReviewResponse;
import com.skipers.skipa.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * @param request 검토 요청
     * @return 생성된 사업부 검토
     */
    @Operation(summary = "사업부 검토 요청", description = "Legal 팀이 특허를 담당 부서에 검토 요청합니다.")
    @PostMapping("/patents/{patentId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @PathVariable Long patentId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(reviewService.create(patentId, request)));
    }
}
