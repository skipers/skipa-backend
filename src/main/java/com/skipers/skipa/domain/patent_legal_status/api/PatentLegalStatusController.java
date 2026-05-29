package com.skipers.skipa.domain.patent_legal_status.api;

import com.skipers.skipa.domain.patent_legal_status.application.PatentLegalStatusService;
import com.skipers.skipa.domain.patent_legal_status.dto.request.PatentLegalStatusCreateRequest;
import com.skipers.skipa.domain.patent_legal_status.dto.response.PatentLegalStatusResponse;
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
@RequestMapping("/patents/{patentId}/legal-status")
public class PatentLegalStatusController {

    private final PatentLegalStatusService patentLegalStatusService;

    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<PatentLegalStatusResponse>> create(
            @PathVariable Long patentId,
            @Valid @RequestBody PatentLegalStatusCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(patentLegalStatusService.create(patentId, request)));
    }

    @PreAuthorize("hasAnyRole('LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<PatentLegalStatusResponse>> getAll(
            @PathVariable Long patentId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(patentLegalStatusService.getAll(patentId, pageable)));
    }
}
