/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 부서(사업부) 관련 HTTP API 엔드포인트를 제공한다.
 * 역할: 요청을 검증/매핑하고 `DepartmentService`로 위임한 뒤 공통 응답 형태로 반환한다.
 *
 * 사용법:
 * - 부서 생성: `POST /departments`
 * - 부서 조회: `GET /departments/{departmentId}`
 * - 부서 수정: `PUT /departments/{departmentId}`
 * - 부서 삭제: `DELETE /departments/{departmentId}`
 * - 부서 목록/검색(page/size): `GET /departments?keyword=&page=&size=`
 */
package com.skipers.skipa.domain.department.api;

import com.skipers.skipa.domain.department.application.DepartmentService;
import com.skipers.skipa.domain.department.dto.request.DepartmentCreateRequest;
import com.skipers.skipa.domain.department.dto.request.DepartmentUpdateRequest;
import com.skipers.skipa.domain.department.dto.response.DepartmentResponse;
import com.skipers.skipa.global.response.ApiResponse;
import com.skipers.skipa.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부서(사업부) Controller.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/departments")
public class DepartmentController {

    /** 부서 서비스. */
    private final DepartmentService departmentService;

    /**
     * 부서를 생성한다.
     *
     * @param request 생성 요청
     * @return 생성된 부서
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@RequestBody DepartmentCreateRequest request) {
        DepartmentResponse response = departmentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 부서를 ID로 조회한다.
     *
     * @param departmentId 부서 ID
     * @return 부서
     */
    @GetMapping("/{departmentId}")
    public ApiResponse<DepartmentResponse> get(@PathVariable Long departmentId) {
        DepartmentResponse response = departmentService.get(departmentId);
        return ApiResponse.success(response);
    }

    /**
     * 부서를 부서명으로 조회한다.
     *
     * @param name 부서명
     * @return 부서
     */
    @GetMapping("/by-name")
    public ApiResponse<DepartmentResponse> getByName(@RequestParam String name) {
        DepartmentResponse response = departmentService.getByName(name);
        return ApiResponse.success(response);
    }

    /**
     * 부서를 수정한다(현재는 부서명만 변경).
     *
     * @param departmentId 부서 ID
     * @param request 수정 요청
     * @return 수정된 부서
     */
    @PutMapping("/{departmentId}")
    public ApiResponse<DepartmentResponse> update(
            @PathVariable Long departmentId,
            @RequestBody DepartmentUpdateRequest request
    ) {
        DepartmentResponse response = departmentService.update(departmentId, request);
        return ApiResponse.success(response);
    }

    /**
     * 부서를 삭제한다.
     *
     * @param departmentId 부서 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> delete(@PathVariable Long departmentId) {
        departmentService.delete(departmentId);
        return ApiResponse.success(null);
    }

    /**
     * 부서 목록/검색을 조회한다(page/size 기반).
     *
     * @param keyword 부서명 검색 키워드(선택)
     * @param pageable page/size 정보
     * @return 부서 목록 페이지
     */
    @GetMapping
    public ApiResponse<PageResponse<DepartmentResponse>> search(
            @RequestParam(required = false) String keyword,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        Page<DepartmentResponse> page = departmentService.searchPage(keyword, pageable);
        return ApiResponse.success(PageResponse.from(page));
    }
}
