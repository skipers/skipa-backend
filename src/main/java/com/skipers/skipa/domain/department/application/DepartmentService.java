/*
 * 작성자: 고길훈
 * 작성일: 2026-05-22 (Asia/Seoul)
 * 목적: 부서(사업부) 도메인의 핵심 비즈니스 로직을 제공한다.
 * 역할: 부서 생성/조회/수정/삭제 및 검색 기능을 묶어 Service 계층에서 제공한다.
 *
 * 사용법:
 * - Controller에서 `DepartmentService`를 주입받아 메서드를 호출한다.
 * - DB 접근은 `DepartmentRepository`를 통해 수행한다.
 */
package com.skipers.skipa.domain.department.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.dto.request.DepartmentCreateRequest;
import com.skipers.skipa.domain.department.dto.request.DepartmentUpdateRequest;
import com.skipers.skipa.domain.department.dto.response.DepartmentResponse;
import com.skipers.skipa.domain.department.exception.DepartmentNotFoundException;
import com.skipers.skipa.global.exception.BusinessException;
import com.skipers.skipa.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부서(사업부) Service.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    /** 부서 저장소. */
    private final DepartmentRepository departmentRepository;

    /**
     * 부서를 생성한다.
     *
     * @param request 부서 생성 요청
     * @return 생성된 부서 응답
     */
    public DepartmentResponse create(DepartmentCreateRequest request) {
        String name = normalizeName(request.getName());

        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_DEPARTMENT_NAME, "이미 존재하는 부서명입니다. name=" + name);
        }

        Department department = departmentRepository.save(new Department(name));
        return DepartmentResponse.from(department);
    }

    /**
     * 부서를 ID로 조회한다.
     *
     * @param departmentId 부서 ID
     * @return 부서 응답
     */
    @Transactional(readOnly = true)
    public DepartmentResponse get(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> DepartmentNotFoundException.withId(departmentId));

        return DepartmentResponse.from(department);
    }

    /**
     * 부서를 부서명으로 조회한다.
     *
     * @param name 부서명
     * @return 부서 응답
     */
    @Transactional(readOnly = true)
    public DepartmentResponse getByName(String name) {
        String normalizedName = normalizeName(name);

        Department department = departmentRepository.findByNameIgnoreCase(normalizedName)
                .orElseThrow(() -> DepartmentNotFoundException.withName(normalizedName));

        return DepartmentResponse.from(department);
    }

    /**
     * 부서를 수정한다(현재는 부서명만 변경).
     *
     * @param departmentId 부서 ID
     * @param request 수정 요청
     * @return 수정된 부서 응답
     */
    public DepartmentResponse update(Long departmentId, DepartmentUpdateRequest request) {
        String name = normalizeName(request.getName());

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> DepartmentNotFoundException.withId(departmentId));

        if (!department.getName().equalsIgnoreCase(name) && departmentRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_DEPARTMENT_NAME, "이미 존재하는 부서명입니다. name=" + name);
        }

        department.changeName(name);
        return DepartmentResponse.from(department);
    }

    /**
     * 부서를 삭제한다.
     *
     * @param departmentId 부서 ID
     */
    public void delete(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw DepartmentNotFoundException.withId(departmentId);
        }

        departmentRepository.deleteById(departmentId);
    }

    /**
     * 키워드로 부서 목록을 검색한다(전체 반환).
     *
     * @param keyword 검색 키워드(부서명 기준)
     * @return 검색 결과 목록
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> search(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);

        List<Department> departments = normalizedKeyword == null
                ? departmentRepository.findAll()
                : departmentRepository.findByNameContainingIgnoreCase(normalizedKeyword);

        return departments.stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    /**
     * 키워드로 부서 목록을 검색한다(page/size 기반).
     *
     * @param keyword 검색 키워드(부서명 기준)
     * @param pageable page/size 정보
     * @return 검색 결과 페이지
     */
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> searchPage(String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);

        Page<Department> page = normalizedKeyword == null
                ? departmentRepository.findAll(pageable)
                : departmentRepository.findByNameContainingIgnoreCase(normalizedKeyword, pageable);

        return page.map(DepartmentResponse::from);
    }

    /** 부서명을 저장/비교하기 좋은 형태로 정규화한다. */
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

    /** 검색 키워드를 정규화한다(없으면 null). */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
