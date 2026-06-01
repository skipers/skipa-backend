package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.PatentService;
import com.skipers.skipa.domain.patent.dto.request.PatentDepartmentChangeRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentCreateRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentUpdateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentListResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patents")
public class PatentController {

    private final PatentService patentService;

    /**
     * 특허를 생성한다.
     *
     * @param request 생성 요청
     * @return 생성된 특허
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<PatentDetailResponse>> create(@Valid @RequestBody PatentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(patentService.create(request)));
    }

    /**
     * 특허를 ID로 조회한다.
     *
     * @param patentId 특허 ID
     * @return 특허
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/{patentId}")
    public ApiResponse<PatentDetailResponse> get(@PathVariable Long patentId) {
        return ApiResponse.ok(patentService.get(patentId));
    }

    /**
     * 특허 목록을 조회한다(page/size 기반).
     *
     * @param keyword 특허명 검색 키워드(선택)
     * @param pageable page/size 정보
     * @return 특허 목록 페이지
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping
    public ApiResponse<PageResponse<PatentListResponse>> getAll(
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(patentService.getAll(keyword, pageable)));
    }

    /**
     * 특허를 수정한다.
     *
     * @param patentId 특허 ID
     * @param request 수정 요청
     * @return 수정된 특허
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @PutMapping("/{patentId}")
    public ApiResponse<PatentDetailResponse> update(
            @PathVariable Long patentId,
            @Valid @RequestBody PatentUpdateRequest request
    ) {
        return ApiResponse.ok(patentService.update(patentId, request));
    }

    /**
     * 특허 담당 부서를 변경한다.
     *
     * @param patentId 특허 ID
     * @param request 담당 부서 변경 요청
     * @return 변경된 특허
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @PatchMapping("/{patentId}/department")
    public ApiResponse<PatentDetailResponse> changeDepartment(
            @PathVariable Long patentId,
            @Valid @RequestBody PatentDepartmentChangeRequest request
    ) {
        return ApiResponse.ok(patentService.changeDepartment(patentId, request));
    }

    /**
     * 특허를 삭제한다.
     *
     * @param patentId 특허 ID
     * @return 성공 응답
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @DeleteMapping("/{patentId}")
    public ApiResponse<Void> delete(@PathVariable Long patentId) {
        patentService.delete(patentId);
        return ApiResponse.ok();
    }
}
