package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.AssignedPatentService;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/assigned-patents")
public class AssignedPatentController {

    private final AssignedPatentService assignedPatentService;

    /**
     * 본인 소속 부서의 담당 특허 목록을 조회한다(page/size 기반).
     *
     * @param userDetails 인증 사용자 정보
     * @param pageable page/size 정보
     * @return 담당 특허 목록 페이지
     */
    @Operation(summary = "담당 특허 목록 조회", description = "사업부 사용자의 소속 부서에 검토 요청된 담당 특허 목록을 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<AssignedPatentResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(assignedPatentService.getAll(userDetails.getUser(), pageable)));
    }

    /**
     * 본인 소속 부서의 담당 특허를 ID로 조회한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @return 담당 특허 상세 정보
     */
    @Operation(summary = "담당 특허 단일 조회", description = "사업부 사용자의 소속 부서에 검토 요청된 담당 특허와 의견 제출 정보를 조회합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/{patentId}")
    public ApiResponse<AssignedPatentDetailResponse> get(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId
    ) {
        return ApiResponse.ok(assignedPatentService.get(userDetails.getUser(), patentId));
    }

    /**
     * 본인 소속 부서의 담당 특허에 의견을 제출한다.
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @param request 의견 제출 요청
     * @return 의견이 반영된 담당 특허
     */
    @Operation(summary = "담당 특허 의견 제출", description = "사업부 사용자가 담당 특허에 유지 의견 또는 포기 의견을 제출합니다.")
    @PreAuthorize("hasRole('BUSINESS')")
    @PostMapping("/{patentId}/opinions")
    public ApiResponse<AssignedPatentResponse> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @Valid @RequestBody ReviewSubmitRequest request
    ) {
        return ApiResponse.ok(assignedPatentService.submit(userDetails.getUser(), patentId, request));
    }
}
