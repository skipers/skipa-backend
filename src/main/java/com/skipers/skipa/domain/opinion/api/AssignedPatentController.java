package com.skipers.skipa.domain.opinion.api;

import com.skipers.skipa.domain.opinion.application.OpinionSubmissionService;
import com.skipers.skipa.domain.opinion.dto.request.OpinionSubmissionSubmitRequest;
import com.skipers.skipa.domain.opinion.dto.response.OpinionSubmissionDetailResponse;
import com.skipers.skipa.domain.opinion.dto.response.OpinionSubmissionResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
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
@RequestMapping("/assigned-patents") // 사업부 담당 특허
public class AssignedPatentController {

    private final OpinionSubmissionService opinionSubmissionService;

    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<OpinionSubmissionResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(opinionSubmissionService.getAll(userDetails.getUser(), pageable)));
    }

    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/{opinionSubmissionId}")
    public ApiResponse<OpinionSubmissionDetailResponse> get(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long opinionSubmissionId
    ) {
        return ApiResponse.ok(opinionSubmissionService.get(userDetails.getUser(), opinionSubmissionId));
    }

    @PreAuthorize("hasRole('BUSINESS')")
    @PostMapping("/{opinionSubmissionId}/opinions")
    public ApiResponse<OpinionSubmissionResponse> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long opinionSubmissionId,
            @Valid @RequestBody OpinionSubmissionSubmitRequest request
    ) {
        return ApiResponse.ok(opinionSubmissionService.submit(userDetails.getUser(), opinionSubmissionId, request));
    }
}
