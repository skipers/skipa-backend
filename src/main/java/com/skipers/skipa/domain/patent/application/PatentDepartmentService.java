package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.patent.dao.PatentDepartmentRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentDepartment;
import com.skipers.skipa.domain.patent.dto.request.PatentDepartmentAssignRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDepartmentResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatentDepartmentService {

    private final PatentDepartmentRepository patentDepartmentRepository;
    private final PatentRepository patentRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public PatentDepartmentResponse assign(Long patentId, PatentDepartmentAssignRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new DepartmentException(ErrorCode.DEPARTMENT_NOT_FOUND));

        PatentDepartment patentDepartment = patentDepartmentRepository.save(PatentDepartment.builder()
                .patent(patent)
                .department(department)
                .assignedAt(Instant.now())
                .build());

        return PatentDepartmentResponse.from(patentDepartment);
    }

    public PatentDepartmentResponse getCurrent(Long patentId) {
        if (!patentRepository.existsById(patentId)) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }

        PatentDepartment patentDepartment = patentDepartmentRepository.findFirstByPatentIdOrderByAssignedAtDesc(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_DEPARTMENT_NOT_FOUND));

        return PatentDepartmentResponse.from(patentDepartment);
    }

    public Page<PatentDepartmentResponse> getAll(Long patentId, Pageable pageable) {
        if (!patentRepository.existsById(patentId)) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "assignedAt")
        );

        return patentDepartmentRepository.findByPatentId(patentId, sortedPageable).map(PatentDepartmentResponse::from);
    }
}
