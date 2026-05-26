package com.skipers.skipa.domain.department.api;

import com.skipers.skipa.domain.department.application.DepartmentService;
import com.skipers.skipa.domain.department.dto.request.DepartmentCreateRequest;
import com.skipers.skipa.domain.department.dto.request.DepartmentUpdateRequest;
import com.skipers.skipa.domain.department.dto.response.DepartmentResponse;
import com.skipers.skipa.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

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
    @GetMapping("/{departmentId}")
    public ApiResponse<DepartmentResponse> get(@PathVariable Long departmentId) {
        return ApiResponse.ok(departmentService.get(departmentId));
    }

    /**
     * 부서를 부서명으로 조회한다.
     *
     * @param name 부서명
     * @return 부서
     */
    @GetMapping("/by-name")
    public ApiResponse<DepartmentResponse> getByName(@RequestParam String name) {
        return ApiResponse.ok(departmentService.getByName(name));
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
            @Valid @RequestBody DepartmentUpdateRequest request
    ) {
        return ApiResponse.ok(departmentService.update(departmentId, request));
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
        return ApiResponse.ok(null);
    }

    /**
     * 부서 목록/검색을 조회한다.
     *
     * @param keyword 부서명 검색 키워드(선택)
     * @return 부서 목록
     */
    @GetMapping
    public ApiResponse<List<DepartmentResponse>> search(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(departmentService.search(keyword));
    }
}
