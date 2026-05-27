package com.skipers.skipa.domain.department.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.dto.request.DepartmentCreateRequest;
import com.skipers.skipa.domain.department.dto.request.DepartmentUpdateRequest;
import com.skipers.skipa.domain.department.dto.response.DepartmentResponse;
import com.skipers.skipa.domain.department.exception.DepartmentNotFoundException;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import com.skipers.skipa.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest request) {
        String name = normalizeName(request.name());

        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
        }

        Department department = departmentRepository.save(Department.builder()
                .name(name)
                .build());
        return DepartmentResponse.from(department);
    }

    public DepartmentResponse get(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(DepartmentNotFoundException::new);

        return DepartmentResponse.from(department);
    }

    public Page<DepartmentResponse> getAll(String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);

        int pageNumber = normalizePage(page);
        int pageSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        Page<DepartmentResponse> result = normalizedKeyword == null
                ? departmentRepository.findAll(pageRequest).map(DepartmentResponse::from)
                : departmentRepository.findByNameContainingIgnoreCase(normalizedKeyword, pageRequest).map(DepartmentResponse::from);

        return PageResponse.from(result);
    }

    @Transactional
    public DepartmentResponse update(Long departmentId, DepartmentUpdateRequest request) {
        String name = normalizeName(request.name());

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(DepartmentNotFoundException::new);

        if (!department.getName().equalsIgnoreCase(name) && departmentRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
        }

        department.updateName(name);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public void delete(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new DepartmentNotFoundException();
        }

        if (userRepository.existsByDepartmentId(departmentId)) {
            throw new BusinessException(ErrorCode.DEPARTMENT_IN_USE);
        }

        departmentRepository.deleteById(departmentId);
    }
  
    private String normalizeName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("부서명은 필수입니다.");
        }

        String normalized = name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("부서명은 공백일 수 없습니다.");
        }

        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return size;
    }
}
