package com.skipers.skipa.domain.patent.api;

import com.skipers.skipa.domain.patent.application.PatentDepartmentService;
import com.skipers.skipa.domain.patent.dto.request.PatentDepartmentAssignRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDepartmentResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/patents/{patentId}/departments") // 특허 담당 부서
public class PatentDepartmentController {

    private final PatentDepartmentService patentDepartmentService;

    @PreAuthorize("hasRole('LEGAL')")
    @PostMapping
    public ResponseEntity<ApiResponse<PatentDepartmentResponse>> assign(
            @PathVariable Long patentId,
            @Valid @RequestBody PatentDepartmentAssignRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(patentDepartmentService.assign(patentId, request)));
    }

    @PreAuthorize("hasRole('LEGAL')")
    @PutMapping("/{deptId}")
    public ResponseEntity<ApiResponse<PatentDepartmentResponse>> change(
            @PathVariable Long patentId,
            @PathVariable Long deptId,
            @Valid @RequestBody PatentDepartmentAssignRequest request
    ) {
        PatentDepartmentResponse current = patentDepartmentService.getCurrent(patentId);
        PatentDepartmentResponse changed = patentDepartmentService.change(patentId, deptId, request);

        if (changed.id().equals(current.id())) {
            return ResponseEntity.ok(ApiResponse.ok(changed));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(changed));
    }

    @PreAuthorize("hasRole('LEGAL')")
    @DeleteMapping("/{deptId}")
    public ApiResponse<Void> unassign(
            @PathVariable Long patentId,
            @PathVariable Long deptId
    ) {
        patentDepartmentService.unassign(patentId, deptId);
        return ApiResponse.ok();
    }

    @PreAuthorize("hasRole('LEGAL')")
    @GetMapping
    public ApiResponse<PageResponse<PatentDepartmentResponse>> getAll(
            @PathVariable Long patentId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ApiResponse.ok(PageResponse.from(patentDepartmentService.getAll(patentId, pageable)));
    }

    @PreAuthorize("hasRole('LEGAL')")
    @GetMapping("/current")
    public ApiResponse<PatentDepartmentResponse> getCurrent(@PathVariable Long patentId) {
        return ApiResponse.ok(patentDepartmentService.getCurrent(patentId));
    }
}
