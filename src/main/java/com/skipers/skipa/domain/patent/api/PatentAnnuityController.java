package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.PatentAnnuityService;
import com.skipers.skipa.domain.patent.dto.request.PatentAnnuityCreateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentAnnuityResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import com.skipers.skipa.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/patents/{patentId}/annuities") // 연차료 납부 이력
public class PatentAnnuityController {

    private final PatentAnnuityService patentAnnuityService;

    /**
     * 특허의 연차료 납부 이력을 생성한다.
     *
     * @param patentId 특허 ID
     * @param request 생성 요청
     * @return 생성된 연차료 납부 이력
     */
    @Operation(summary = "연차료 납부 이력 생성", description = "특허에 연차료 납부 이력을 추가합니다.")
    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<PatentAnnuityResponse>> create(
            @PathVariable Long patentId,
            @Valid @RequestBody PatentAnnuityCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(patentAnnuityService.create(patentId, request)));
    }

    /**
     * 특허의 연차료 납부 이력 목록을 조회한다(page/size 기반).
     *
     * @param userDetails 인증 사용자 정보
     * @param patentId 특허 ID
     * @param pageable page/size 정보
     * @return 연차료 납부 이력 목록 페이지
     */
    @Operation(summary = "연차료 납부 이력 조회", description = "특허의 연차료 납부 이력을 페이지 단위로 조회합니다.")
    @PreAuthorize("hasAnyRole('LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<PatentAnnuityResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(patentAnnuityService.getAll(userDetails.getUser(), patentId, pageable)));
    }
}
