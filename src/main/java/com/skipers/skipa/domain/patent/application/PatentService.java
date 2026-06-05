package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.dto.request.PatentCreateRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentDepartmentChangeRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentUpdateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentListResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatentService {

    private final PatentRepository patentRepository;
    private final DepartmentRepository departmentRepository;
    private final PatentLegalStatusRepository patentLegalStatusRepository;
    private final PatentAnnuityRepository patentAnnuityRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final BusinessPatentAccessValidator businessPatentAccessValidator;
    @Transactional
    public PatentDetailResponse create(PatentCreateRequest request) {
        String applicationNumber = request.applicationNumber();

        if (patentRepository.existsByApplicationNumber(applicationNumber)) {
            throw new PatentException(ErrorCode.DUPLICATE_APPLICATION_NUMBER);
        }

        Patent patent = patentRepository.save(Patent.builder()
                .title(request.title())
                .applicationNumber(applicationNumber)
                .registrationNumber(request.registrationNumber())
                .publicationNumber(request.publicationNumber())
                .announcementNumber(request.announcementNumber())
                .applicationDate(request.applicationDate())
                .registrationDate(request.registrationDate())
                .publicationDate(request.publicationDate())
                .announcementDate(request.announcementDate())
                .ipcCode(request.ipcCode())
                .cpcCode(request.cpcCode())
                .applicant(request.applicant())
                .inventor(request.inventor())
                .expiryDate(request.expiryDate())
                .citationCount(request.citationCount())
                .originalPdfKey(request.originalPdfKey())
                .managementNumber(request.managementNumber())
                .businessField(request.businessField())
                .techField(request.techField())
                .relatedProducts(request.relatedProducts())
                .filingCountry(request.filingCountry())
                .isJointApplication(request.isJointApplication())
                .jointApplicant(request.jointApplicant())
                .initialDepartment(request.initialDepartment())
                .keywords(request.keywords())
                .overview(request.overview())
                .coreContent(request.coreContent())
                .build());

        return toDetailResponse(patent);
    }

    @Transactional
    public PatentDetailResponse changeDepartment(Long patentId, PatentDepartmentChangeRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new DepartmentException(ErrorCode.DEPARTMENT_NOT_FOUND));
        if (department.isInactive()) {
            throw new DepartmentException(ErrorCode.DEPARTMENT_INACTIVE);
        }

        patent.changeCurrentDepartment(department);
        return toDetailResponse(patent);
    }

    public PatentDetailResponse get(User user, Long patentId) {
        businessPatentAccessValidator.validate(user, patentId);

        return get(patentId);
    }

    public PatentDetailResponse get(Long patentId) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        return toDetailResponse(patent);
    }

    public Page<PatentListResponse> getAll(User user, String keyword, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<Patent> patents = user.getRole() == UserRole.BUSINESS
                ? findBusinessPatents(user, normalizedKeyword, sortedPageable)
                : findPatents(normalizedKeyword, sortedPageable);

        return patents.map(PatentListResponse::from);
    }

    @Transactional
    public PatentDetailResponse update(Long patentId, PatentUpdateRequest request) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));
        String applicationNumber = request.applicationNumber();

        if (!patent.getApplicationNumber().equals(applicationNumber)
                && patentRepository.existsByApplicationNumber(applicationNumber)) {
            throw new PatentException(ErrorCode.DUPLICATE_APPLICATION_NUMBER);
        }

        patent.update(
                request.title(),
                applicationNumber,
                request.registrationNumber(),
                request.publicationNumber(),
                request.announcementNumber(),
                request.applicationDate(),
                request.registrationDate(),
                request.publicationDate(),
                request.announcementDate(),
                request.ipcCode(),
                request.cpcCode(),
                request.applicant(),
                request.inventor(),
                request.expiryDate(),
                request.citationCount(),
                request.originalPdfKey(),
                request.managementNumber(),
                request.businessField(),
                request.techField(),
                request.relatedProducts(),
                request.filingCountry(),
                request.isJointApplication(),
                request.jointApplicant(),
                request.initialDepartment(),
                request.keywords(),
                request.overview(),
                request.coreContent()
        );

        return toDetailResponse(patent);
    }

    @Transactional
    public void delete(Long patentId) {
        if (!patentRepository.existsById(patentId)) {
            throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
        }

        patentLegalStatusRepository.deleteAllByPatentId(patentId); // 권리 상태 이력
        patentAnnuityRepository.deleteAllByPatentId(patentId); // 연차료 납부 이력
        reviewRepository.deleteAllByPatentId(patentId); // 사업부 검토
        reportRepository.deleteAllByPatentId(patentId); // 평가 보고서
        patentRepository.deleteById(patentId);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Page<Patent> findBusinessPatents(User user, String keyword, Pageable pageable) {
        if (user.getDepartment() == null) {
            throw new PatentException(ErrorCode.FORBIDDEN);
        }

        Long departmentId = user.getDepartment().getId();
        return keyword == null
                ? patentRepository.findByCurrentDepartmentId(departmentId, pageable)
                : patentRepository.findByCurrentDepartmentIdAndTitleContainingIgnoreCase(departmentId, keyword, pageable);
    }

    private Page<Patent> findPatents(String keyword, Pageable pageable) {
        return keyword == null
                ? patentRepository.findAll(pageable)
                : patentRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    private PatentDetailResponse toDetailResponse(Patent patent) {
        return PatentDetailResponse.from(patent);
    }
}
