package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentApprovalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import com.skipers.skipa.domain.patent.dto.request.PatentCreateRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentDepartmentChangeRequest;
import com.skipers.skipa.domain.patent.dto.request.PatentUpdateRequest;
import com.skipers.skipa.domain.patent.dto.response.PatentDetailResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentListResponse;
import com.skipers.skipa.domain.patent.dto.response.PatentSummaryResponse;
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.patentextract.dao.PatentExtractJobRepository;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;
import com.skipers.skipa.domain.patentextract.exception.PatentExtractException;
import com.skipers.skipa.domain.portfolio.application.PortfolioInsightCacheInvalidator;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private final PatentExtractJobRepository patentExtractJobRepository;
    private final PatentOriginalPdfStorageService patentOriginalPdfStorageService;
    private final PortfolioInsightCacheInvalidator portfolioInsightCacheInvalidator;

    @Transactional
    public PatentDetailResponse create(User user, PatentCreateRequest request) {
        String applicationNumber = request.applicationNumber();

        if (patentRepository.existsByApplicationNumber(applicationNumber)) {
            throw new PatentException(ErrorCode.DUPLICATE_APPLICATION_NUMBER);
        }

        PatentExtractJob extractJob = getCompletedExtractJob(request.extractJobId());
        String originalPdfKey = extractJob == null ? request.originalPdfKey() : null;

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
                .ipcCodes(request.ipcCodes())
                .cpcCodes(request.cpcCodes())
                .applicant(request.applicant())
                .inventor(request.inventor())
                .expiryDate(request.expiryDate())
                .citationCount(request.citationCount())
                .examinationClaimCount(request.examinationClaimCount())
                .originalPdfKey(originalPdfKey)
                .approvalStatus(initialApprovalStatus(user))
                .currentDepartment(initialDepartment(user))
                .managementNumber(request.managementNumber())
                .businessField(request.businessField())
                .techField(request.techField())
                .relatedProducts(request.relatedProducts())
                .filingCountry(request.filingCountry())
                .isJointApplication(request.isJointApplication())
                .jointApplicant(request.jointApplicant())
                .initialDepartment(request.initialDepartment())
                .keywords(request.keywords())
                .summary(request.summary())
                .build());

        if (extractJob != null) {
            assignExtractedStorageKeys(patent, extractJob);
        }

        portfolioInsightCacheInvalidator.evict();
        return toDetailResponse(patent);
    }

    @Transactional
    public PatentDetailResponse approve(Long patentId) {
        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));

        if (patent.getApprovalStatus() != PatentApprovalStatus.PENDING_APPROVAL) {
            throw new PatentException(ErrorCode.PATENT_APPROVAL_NOT_PENDING);
        }

        patent.approve();

        portfolioInsightCacheInvalidator.evict();
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
        portfolioInsightCacheInvalidator.evict();
        return toDetailResponse(patent);
    }

    public PatentDetailResponse get(User user, Long patentId) {
        businessPatentAccessValidator.validate(user, patentId);

        Patent patent = patentRepository.findById(patentId)
                .orElseThrow(() -> new PatentException(ErrorCode.PATENT_NOT_FOUND));
        validateReadablePatent(user, patent);

        return toDetailResponse(patent);
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
                Sort.by(Sort.Direction.ASC, "applicationNumber").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Patent> patents = findPatents(normalizedKeyword, sortedPageable);

        List<PatentListResponse> responses = patents.getContent().stream()
                .filter(patent -> matchesApprovalStatus(patent, PatentApprovalStatus.APPROVED))
                .map(PatentListResponse::from)
                .toList();
        return new PageImpl<>(responses, sortedPageable, responses.size());
    }

    public Page<PatentListResponse> getAll(
            User user,
            String keyword,
            Long departmentId,
            List<String> statuses,
            String filingCountry,
            String sort,
            Pageable pageable
    ) {
        return getAllByApprovalStatus(
                user,
                keyword,
                departmentId,
                statuses,
                filingCountry,
                PatentApprovalStatus.APPROVED,
                sort,
                pageable
        );
    }

    public Page<PatentListResponse> getAssigned(
            User user,
            String keyword,
            String sort,
            Pageable pageable
    ) {
        if (user.getDepartment() == null) {
            throw new PatentException(ErrorCode.FORBIDDEN);
        }

        return getAllByApprovalStatus(
                user,
                keyword,
                user.getDepartment().getId(),
                null,
                null,
                PatentApprovalStatus.APPROVED,
                sort,
                pageable
        );
    }

    private Page<PatentListResponse> getAllByApprovalStatus(
            User user,
            String keyword,
            Long departmentId,
            List<String> statuses,
            String filingCountry,
            PatentApprovalStatus approvalStatus,
            String sort,
            Pageable pageable
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<Patent> patents = findPatents(normalizedKeyword);
        Map<Long, PatentLegalStatusType> latestStatuses = latestLegalStatuses(patentLegalStatusRepository.findAll());
        Set<PatentLegalStatusType> parsedStatuses = parseLegalStatuses(statuses);

        List<PatentListResponse> responses = patents.stream()
                .filter(patent -> matchesApprovalStatus(patent, approvalStatus))
                .filter(patent -> matchesDepartment(patent, departmentId))
                .filter(patent -> matchesLegalStatus(latestStatuses.get(patent.getId()), parsedStatuses))
                .filter(patent -> matchesText(patent.getFilingCountry(), filingCountry))
                .map(patent -> toListResponse(
                        patent,
                        latestStatuses.get(patent.getId())
                ))
                .sorted(listSort(sort))
                .toList();

        return page(responses, pageable);
    }

    public Page<PatentListResponse> getPendingApprovals(
            User user,
            String keyword,
            String sort,
            Pageable pageable
    ) {
        return getAllByApprovalStatus(
                user,
                keyword,
                null,
                null,
                null,
                PatentApprovalStatus.PENDING_APPROVAL,
                sort,
                pageable
        );
    }

    public PatentSummaryResponse getSummary(User user) {
        List<Patent> patents = user.getRole() == UserRole.BUSINESS
                ? findBusinessPatents(user, null)
                : patentRepository.findAll();
        patents = patents.stream()
                .filter(patent -> matchesApprovalStatus(patent, PatentApprovalStatus.APPROVED))
                .toList();
        Map<Long, PatentLegalStatusType> latestStatuses = latestLegalStatuses(patentLegalStatusRepository.findAll());

        long active = patents.stream()
                .filter(patent -> isActiveStatus(latestStatuses.get(patent.getId())))
                .count();

        return new PatentSummaryResponse(active, patents.size() - active);
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
                request.ipcCodes(),
                request.cpcCodes(),
                request.applicant(),
                request.inventor(),
                request.expiryDate(),
                request.citationCount(),
                request.examinationClaimCount(),
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
                request.summary()
        );

        portfolioInsightCacheInvalidator.evict();
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
        portfolioInsightCacheInvalidator.evict();
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
                : patentRepository.searchByCurrentDepartmentIdAndKeyword(departmentId, keyword, pageable);
    }

    private Page<Patent> findPatents(String keyword, Pageable pageable) {
        return keyword == null
                ? patentRepository.findAll(pageable)
                : patentRepository.searchByKeyword(keyword, pageable);
    }

    private List<Patent> findBusinessPatents(User user, String keyword) {
        if (user.getDepartment() == null) {
            throw new PatentException(ErrorCode.FORBIDDEN);
        }

        Long departmentId = user.getDepartment().getId();
        return keyword == null
                ? patentRepository.findByCurrentDepartmentId(departmentId, Pageable.unpaged()).getContent()
                : patentRepository.searchByCurrentDepartmentIdAndKeyword(
                        departmentId,
                        keyword,
                        Pageable.unpaged()
                ).getContent();
    }

    private List<Patent> findPatents(String keyword) {
        return keyword == null
                ? patentRepository.findAll()
                : patentRepository.searchByKeyword(keyword, Pageable.unpaged()).getContent();
    }

    private Map<Long, PatentLegalStatusType> latestLegalStatuses(List<PatentLegalStatus> legalStatuses) {
        Map<Long, PatentLegalStatus> latestByPatentId = new HashMap<>();
        for (PatentLegalStatus legalStatus : legalStatuses) {
            Long patentId = legalStatus.getPatent().getId();
            PatentLegalStatus current = latestByPatentId.get(patentId);
            if (current == null || compareLegalStatusRecency(legalStatus, current) > 0) {
                latestByPatentId.put(patentId, legalStatus);
            }
        }

        Map<Long, PatentLegalStatusType> latestStatuses = new HashMap<>();
        latestByPatentId.forEach((patentId, legalStatus) -> latestStatuses.put(patentId, legalStatus.getStatus()));
        return latestStatuses;
    }

    private Set<PatentLegalStatusType> parseLegalStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return Set.of();
        }

        Set<PatentLegalStatusType> parsedStatuses = new HashSet<>();
        for (String status : statuses) {
            if (status == null || status.isBlank()) {
                continue;
            }
            try {
                parsedStatuses.add(PatentLegalStatusType.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw new PatentException(ErrorCode.INVALID_REQUEST);
            }
        }
        return parsedStatuses;
    }

    private boolean matchesApprovalStatus(Patent patent, PatentApprovalStatus approvalStatus) {
        return patent.getApprovalStatus() == approvalStatus;
    }

    private void validateReadablePatent(User user, Patent patent) {
        if (patent.getApprovalStatus() == PatentApprovalStatus.APPROVED) {
            return;
        }
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.LEGAL) {
            return;
        }
        throw new PatentException(ErrorCode.PATENT_NOT_FOUND);
    }

    private boolean matchesDepartment(Patent patent, Long departmentId) {
        if (departmentId == null) {
            return true;
        }
        Department department = patent.getCurrentDepartment();
        if (departmentId == -1L) {
            return department == null;
        }
        return department != null && department.getId().equals(departmentId);
    }

    private boolean matchesLegalStatus(PatentLegalStatusType latestStatus, Set<PatentLegalStatusType> statuses) {
        return statuses.isEmpty() || latestStatus != null && statuses.contains(latestStatus);
    }

    private boolean isActiveStatus(PatentLegalStatusType latestStatus) {
        return latestStatus == PatentLegalStatusType.APPLIED
                || latestStatus == PatentLegalStatusType.PUBLISHED
                || latestStatus == PatentLegalStatusType.REGISTERED;
    }

    private boolean matchesText(String actual, String expected) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }

    private PatentListResponse toListResponse(
            Patent patent,
            PatentLegalStatusType latestLegalStatus
    ) {
        return PatentListResponse.of(
                patent,
                latestLegalStatus == null ? null : latestLegalStatus.name()
        );
    }

    private Comparator<PatentListResponse> listSort(String sort) {
        PatentSortOption sortOption = PatentSortOption.parse(sort);
        return PatentSortOption.comparator(
                sortOption,
                switch (sortOption.field()) {
                    case "title" -> PatentListResponse::title;
                    case "applicationNumber" -> PatentListResponse::applicationNumber;
                    case "applicationDate" -> PatentListResponse::applicationDate;
                    case "expiryDate" -> PatentListResponse::expiryDate;
                    case "citationCount" -> PatentListResponse::citationCount;
                    case "id" -> PatentListResponse::id;
                    default -> throw new PatentException(ErrorCode.INVALID_REQUEST);
                },
                PatentListResponse::id
        );
    }

    private Page<PatentListResponse> page(List<PatentListResponse> responses, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(responses);
        }
        int start = (int) pageable.getOffset();
        if (start >= responses.size()) {
            return new PageImpl<>(List.of(), pageable, responses.size());
        }
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
    }

    private int compareLegalStatusRecency(PatentLegalStatus left, PatentLegalStatus right) {
        Comparator<PatentLegalStatus> comparator = Comparator
                .comparing(PatentLegalStatus::getChangedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(PatentLegalStatus::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
        return comparator.compare(left, right);
    }

    private PatentExtractJob getCompletedExtractJob(Long extractJobId) {
        if (extractJobId == null) {
            return null;
        }

        PatentExtractJob extractJob = patentExtractJobRepository.findById(extractJobId)
                .orElseThrow(() -> new PatentExtractException(ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND));

        if (!extractJob.isCompleted()) {
            throw new PatentExtractException(ErrorCode.PATENT_EXTRACT_NOT_COMPLETED);
        }

        return extractJob;
    }

    private PatentApprovalStatus initialApprovalStatus(User user) {
        return user.getRole() == UserRole.BUSINESS
                ? PatentApprovalStatus.PENDING_APPROVAL
                : PatentApprovalStatus.APPROVED;
    }

    private Department initialDepartment(User user) {
        return user.getRole() == UserRole.BUSINESS ? user.getDepartment() : null;
    }

    private void assignExtractedStorageKeys(Patent patent, PatentExtractJob extractJob) {
        String originalPdfKey = buildFinalPdfObjectKey(patent.getId());
        String parsedJsonKey = buildParsedJsonObjectKey(patent.getId());

        patentOriginalPdfStorageService.copy(extractJob.getObjectKey(), originalPdfKey);
        patentOriginalPdfStorageService.saveJson(parsedJsonKey, extractJob.getResultJson());
        patent.assignStorageKeys(originalPdfKey, parsedJsonKey);
    }

    private String buildFinalPdfObjectKey(Long patentId) {
        return "patents/%d/original.pdf".formatted(patentId);
    }

    private String buildParsedJsonObjectKey(Long patentId) {
        return "patents/%d/parsed.json".formatted(patentId);
    }

    private PatentDetailResponse toDetailResponse(Patent patent) {
        String latestLegalStatus = patentLegalStatusRepository
                .findFirstByPatentIdOrderByChangedAtDescIdDesc(patent.getId())
                .map(legalStatus -> legalStatus.getStatus().name())
                .orElse(null);
        BigDecimal latestReportScore = reportRepository
                .findFirstByPatentIdAndStatusInOrderByIdDesc(
                        patent.getId(),
                        List.of(
                                com.skipers.skipa.domain.report.domain.ReportStatus.REPORT_COMPLETED,
                                com.skipers.skipa.domain.report.domain.ReportStatus.EMBEDDING_COMPLETED
                        )
                )
                .map(Report::getTotalScore)
                .orElse(null);
        return PatentDetailResponse.of(patent, latestLegalStatus, latestReportScore);
    }

}
