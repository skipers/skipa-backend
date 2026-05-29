package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.AnnuityHistoryService;
import com.skipers.skipa.domain.patent.dto.request.AnnuityCreateRequest;
import com.skipers.skipa.domain.patent.dto.response.AnnuityResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patents/{patentId}/annuities") // 연차료 납부 이력
public class PatentAnnuityController {

    private final AnnuityHistoryService annuityHistoryService;

    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<AnnuityResponse>> create(
            @PathVariable Long patentId,
            @Valid @RequestBody AnnuityCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(annuityHistoryService.create(patentId, request)));
    }

    @PreAuthorize("hasAnyRole('LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<AnnuityResponse>> getAll(
            @PathVariable Long patentId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(annuityHistoryService.getAll(patentId, pageable)));
    }
}
