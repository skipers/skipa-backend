package com.skipers.skipa.domain.department.api;

import com.skipers.skipa.domain.department.application.DepartmentService;
import com.skipers.skipa.domain.department.dto.request.DepartmentCreateRequest;
import com.skipers.skipa.domain.department.dto.request.DepartmentUpdateRequest;
import com.skipers.skipa.domain.department.dto.response.DepartmentResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * 부서를 생성한다.
     *
     * @param request 생성 요청
     * @return 생성된 부서
     */
    @Operation(summary = "[Legal] 부서 생성", description = "관리자 또는 Legal 팀이 새로운 부서를 생성합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(departmentService.create(request)));
    }

    /**
     * 부서를 ID로 조회한다.
     *
     * @param departmentId 부서 ID
     * @return 부서
     */
    @Operation(summary = "[Common] 부서 단일 조회", description = "부서 ID로 부서 정보를 조회합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping("/{departmentId}")
    public ApiResponse<DepartmentResponse> get(@PathVariable Long departmentId) {
        return ApiResponse.ok(departmentService.get(departmentId));
    }

    /**
     * 부서 목록을 조회한다(page/size 기반).
     *
     * @param keyword 부서명 검색 키워드(선택)
     * @param pageable page/size 정보
     * @return 부서 목록 페이지
     */
    @Operation(
            summary = "[Common] 부서 목록 조회",
            description = "활성 부서 목록을 페이지 단위로 조회합니다. "
                    + "필터: keyword(부서명 부분 검색). "
                    + "정렬: 부서명 오름차순(name ASC) 고정입니다."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @GetMapping
    public ApiResponse<PageResponse<DepartmentResponse>> getAll(
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 50) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(departmentService.getAll(keyword, pageable)));
    }

    /**
     * 부서를 수정한다.
     *
     * @param departmentId 부서 ID
     * @param request 수정 요청
     * @return 수정된 부서
     */
    @Operation(summary = "[Legal] 부서 수정", description = "관리자 또는 Legal 팀이 부서 ID에 해당하는 부서 정보를 수정합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @PutMapping("/{departmentId}")
    public ApiResponse<DepartmentResponse> update(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentUpdateRequest request
    ) {
        return ApiResponse.ok(departmentService.update(departmentId, request));
    }

    /**
     * 부서를 비활성화한다.
     *
     * @param departmentId 부서 ID
     * @return 성공 응답
     */
    @Operation(summary = "[Legal] 부서 비활성화", description = "관리자 또는 Legal 팀이 부서 ID에 해당하는 부서를 비활성화합니다.")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEGAL')")
    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> delete(@PathVariable Long departmentId) {
        departmentService.delete(departmentId);
        return ApiResponse.ok();
    }
}
