package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.PatentService;
import com.skipers.skipa.domain.patent.dto.request.PatentDepartmentChangeRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentCreateRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentUpdateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentListResponse;
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

import java.util.List;

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
    @Operation(summary = "특허 생성", description = "새로운 특허 정보를 생성합니다.")
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
    @Operation(summary = "특허 단일 조회", description = "특허 ID로 상세 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping("/{patentId}")
    public ApiResponse<PatentDetailResponse> get(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long patentId
    ) {
        return ApiResponse.ok(patentService.get(userDetails.getUser(), patentId));
    }

    /**
     * 특허 목록을 조회한다(page/size 기반).
     *
     * @param keyword 특허명 검색 키워드(선택)
     * @param departmentId 부서 ID(선택, -1은 미배정)
     * @param reviewStatus 재평가 상태(선택)
     * @param decision 사업부 결정(선택)
     * @param checked 회신 확인 여부(선택)
     * @param status 권리 상태(선택, 복수 가능)
     * @param filingCountry 출원국(선택)
     * @param techField 기술 분야(선택)
     * @param sort 정렬 기준(선택)
     * @param pageable page/size 정보
     * @return 특허 목록 페이지
     */
    @Operation(summary = "특허 목록 조회", description = "특허 목록을 페이지 단위로 조회합니다. 특허명으로 검색할 수 있습니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL', 'BUSINESS')")
    @GetMapping
    public ApiResponse<PageResponse<PatentListResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) Boolean checked,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String filingCountry,
            @RequestParam(required = false) String techField,
            @RequestParam(required = false) String sort,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(patentService.getAll(
                userDetails.getUser(),
                keyword,
                departmentId,
                reviewStatus,
                decision,
                checked,
                status,
                filingCountry,
                techField,
                sort,
                pageable
        )));
    }

    /**
     * 특허를 수정한다.
     *
     * @param patentId 특허 ID
     * @param request 수정 요청
     * @return 수정된 특허
     */
    @Operation(summary = "특허 수정", description = "특허 ID에 해당하는 특허 정보를 수정합니다.")
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
    @Operation(summary = "특허 담당 부서 변경", description = "특허의 현재 담당 부서를 변경합니다.")
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
    @Operation(summary = "특허 삭제", description = "특허 ID에 해당하는 특허와 연관 데이터를 삭제합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @DeleteMapping("/{patentId}")
    public ApiResponse<Void> delete(@PathVariable Long patentId) {
        patentService.delete(patentId);
        return ApiResponse.ok();
    }
}
