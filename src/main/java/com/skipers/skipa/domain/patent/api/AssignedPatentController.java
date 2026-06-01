package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.AssignedPatentService;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.AssignedPatentResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import com.skipers.skipa.domain.review.dto.request.ReviewSubmitRequest;
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

    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<AssignedPatentResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(assignedPatentService.getAll(userDetails.getUser(), pageable)));
    }

    @PreAuthorize("hasRole('BUSINESS')")
    @GetMapping("/{patentId}")
    public ApiResponse<AssignedPatentDetailResponse> get(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId
    ) {
        return ApiResponse.ok(assignedPatentService.get(userDetails.getUser(), patentId));
    }

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
