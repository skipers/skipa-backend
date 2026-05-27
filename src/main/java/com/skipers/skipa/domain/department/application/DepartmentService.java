package com.skipers.skipa.domain.department.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.dto.request.DepartmentCreateRequest;
import com.skipers.skipa.domain.department.dto.request.DepartmentUpdateRequest;
import com.skipers.skipa.domain.department.dto.response.DepartmentResponse;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        String name = request.name();

        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DepartmentException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
        }

        Department department = departmentRepository.save(Department.builder()
                .name(name)
                .build());

        return DepartmentResponse.from(department);
    }

    public DepartmentResponse get(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentException(ErrorCode.DEPARTMENT_NOT_FOUND));

        return DepartmentResponse.from(department);
    }

    public Page<DepartmentResponse> getAll(String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "name")
        );

        Page<DepartmentResponse> result = normalizedKeyword == null
                ? departmentRepository.findAll(sortedPageable).map(DepartmentResponse::from)
                : departmentRepository.findByNameContainingIgnoreCase(normalizedKeyword, sortedPageable).map(DepartmentResponse::from);

        return result;
    }

    @Transactional
    public DepartmentResponse update(Long departmentId, DepartmentUpdateRequest request) {
        String name = request.name();

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentException(ErrorCode.DEPARTMENT_NOT_FOUND));

        if (!department.getName().equalsIgnoreCase(name) && departmentRepository.existsByNameIgnoreCase(name)) {
            throw new DepartmentException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
        }

        department.update(name);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public void delete(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }

        if (userRepository.existsByDepartmentId(departmentId)) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_IN_USE);
        }

        departmentRepository.deleteById(departmentId);
    }
  
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
