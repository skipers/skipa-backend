package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.patent.dao.PatentAnnuityRepository;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
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
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
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
    private final ReviewCycleRepository reviewCycleRepository;
    private final ReportRepository reportRepository;
    private final BusinessPatentAccessValidator businessPatentAccessValidator;
    private final PatentExtractJobRepository patentExtractJobRepository;
    private final PatentOriginalPdfStorageService patentOriginalPdfStorageService;

    @Transactional
    public PatentDetailResponse create(PatentCreateRequest request) {
        String applicationNumber = request.applicationNumber();

        if (patentRepository.existsByApplicationNumber(applicationNumber)) {
            throw new PatentException(ErrorCode.DUPLICATE_APPLICATION_NUMBER);
        }

        String originalPdfKey = resolveOriginalPdfKey(request);

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
                .examinationClaimCount(request.examinationClaimCount())
                .originalPdfKey(originalPdfKey)
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

    public Page<PatentListResponse> getAll(
            User user,
            String keyword,
            Long departmentId,
            String reviewStatus,
            String decision,
            Boolean checked,
            List<String> statuses,
            String filingCountry,
            String techField,
            String sort,
            Pageable pageable
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<Patent> patents = user.getRole() == UserRole.BUSINESS
                ? findBusinessPatents(user, normalizedKeyword)
                : findPatents(normalizedKeyword);
        Map<Long, PatentLegalStatusType> latestStatuses = latestLegalStatuses(patentLegalStatusRepository.findAll());
        Optional<ReviewCycle> activeReviewCycle = findActiveReviewCycle();
        Map<Long, Review> reviewsByPatentId = activeReviewCycle
                .map(reviewCycle -> latestReviewsByPatentId(reviewRepository.findAllByReviewCycleId(reviewCycle.getId())))
                .orElseGet(Map::of);
        Map<Long, BigDecimal> latestReportScores = latestReportScoresByPatentId();
        Set<PatentLegalStatusType> parsedStatuses = parseLegalStatuses(statuses);
        BusinessOpinion parsedDecision = parseDecision(decision);
        LocalDate today = LocalDate.now();

        List<PatentListResponse> responses = patents.stream()
                .filter(patent -> matchesDepartment(patent, departmentId))
                .filter(patent -> matchesReviewStatus(patent, reviewsByPatentId.get(patent.getId()), reviewStatus, today))
                .filter(patent -> matchesDecision(reviewsByPatentId.get(patent.getId()), parsedDecision))
                .filter(patent -> matchesChecked(reviewsByPatentId.get(patent.getId()), checked))
                .filter(patent -> matchesLegalStatus(latestStatuses.get(patent.getId()), parsedStatuses))
                .filter(patent -> matchesText(patent.getFilingCountry(), filingCountry))
                .filter(patent -> matchesText(patent.getTechField(), techField))
                .map(patent -> toListResponse(
                        patent,
                        latestStatuses.get(patent.getId()),
                        reviewsByPatentId.get(patent.getId()),
                        latestReportScores.get(patent.getId()),
                        today
                ))
                .sorted(listSort(sort))
                .toList();

        return page(responses, pageable);
    }

    public PatentSummaryResponse getSummary(User user) {
        List<Patent> patents = user.getRole() == UserRole.BUSINESS
                ? findBusinessPatents(user, null)
                : patentRepository.findAll();
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
                request.ipcCode(),
                request.cpcCode(),
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

    private List<Patent> findBusinessPatents(User user, String keyword) {
        if (user.getDepartment() == null) {
            throw new PatentException(ErrorCode.FORBIDDEN);
        }

        Long departmentId = user.getDepartment().getId();
        return keyword == null
                ? patentRepository.findByCurrentDepartmentId(departmentId, Pageable.unpaged()).getContent()
                : patentRepository.findByCurrentDepartmentIdAndTitleContainingIgnoreCase(
                        departmentId,
                        keyword,
                        Pageable.unpaged()
                ).getContent();
    }

    private List<Patent> findPatents(String keyword) {
        return keyword == null
                ? patentRepository.findAll()
                : patentRepository.findByTitleContainingIgnoreCase(keyword, Pageable.unpaged()).getContent();
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

    private Optional<ReviewCycle> findActiveReviewCycle() {
        LocalDate today = LocalDate.now();
        return reviewCycleRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(today, today);
    }

    private Map<Long, Review> latestReviewsByPatentId(List<Review> reviews) {
        Map<Long, Review> latestReviews = new HashMap<>();
        for (Review review : reviews) {
            Long patentId = review.getPatent().getId();
            Review current = latestReviews.get(patentId);
            if (current == null || review.getId() > current.getId()) {
                latestReviews.put(patentId, review);
            }
        }
        return latestReviews;
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

    private BusinessOpinion parseDecision(String decision) {
        if (decision == null || decision.isBlank()) {
            return null;
        }
        try {
            return BusinessOpinion.valueOf(decision);
        } catch (IllegalArgumentException e) {
            throw new PatentException(ErrorCode.INVALID_REQUEST);
        }
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

    private boolean matchesReviewStatus(Patent patent, Review review, String reviewStatus, LocalDate today) {
        if (reviewStatus == null || reviewStatus.isBlank()) {
            return true;
        }
        return switch (reviewStatus) {
            case "unassigned" -> patent.getCurrentDepartment() == null;
            case "unrequested" -> review == null;
            case "unread" -> review != null
                    && review.getStatus() == ReviewStatus.SUBMITTED
                    && !review.isChecked();
            case "requested" -> review != null
                    && review.getStatus() == ReviewStatus.PENDING
                    && !review.getDueDate().isBefore(today);
            case "overdue" -> review != null
                    && review.getStatus() == ReviewStatus.PENDING
                    && review.getDueDate().isBefore(today);
            case "done" -> review != null && review.getStatus() == ReviewStatus.SUBMITTED;
            default -> throw new PatentException(ErrorCode.INVALID_REQUEST);
        };
    }

    private boolean matchesDecision(Review review, BusinessOpinion decision) {
        return decision == null || review != null && review.getOpinion() == decision;
    }

    private boolean matchesChecked(Review review, Boolean checked) {
        return checked == null
                || review != null
                && review.getStatus() == ReviewStatus.SUBMITTED
                && review.isChecked() == checked;
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
            PatentLegalStatusType latestLegalStatus,
            Review review,
            BigDecimal latestReportScore,
            LocalDate today
    ) {
        String reviewStatus = reviewStatus(patent, review, today);
        return PatentListResponse.of(
                patent,
                latestLegalStatus == null ? null : latestLegalStatus.name(),
                reviewStatus,
                review == null || review.getOpinion() == null ? null : review.getOpinion().name(),
                review == null || review.getStatus() != ReviewStatus.SUBMITTED ? null : review.isChecked(),
                latestReportScore,
                review != null && review.getStatus() == ReviewStatus.PENDING && review.getDueDate().isBefore(today)
        );
    }

    private String reviewStatus(Patent patent, Review review, LocalDate today) {
        if (patent.getCurrentDepartment() == null) {
            return "unassigned";
        }
        if (review == null) {
            return "unrequested";
        }
        if (review.getStatus() == ReviewStatus.SUBMITTED) {
            return "done";
        }
        if (review.getStatus() == ReviewStatus.PENDING && review.getDueDate().isBefore(today)) {
            return "overdue";
        }
        if (review.getStatus() == ReviewStatus.PENDING) {
            return "requested";
        }
        return null;
    }

    private Comparator<PatentListResponse> listSort(String sort) {
        Comparator<PatentListResponse> comparator = switch (sort == null ? "" : sort) {
            case "expiryDate" -> Comparator.comparing(
                    PatentListResponse::expiryDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "applicationDate" -> Comparator.comparing(
                    PatentListResponse::applicationDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "citationCount" -> Comparator.comparing(
                    PatentListResponse::citationCount,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ).reversed();
            case "", "id" -> Comparator.comparing(PatentListResponse::id).reversed();
            default -> throw new PatentException(ErrorCode.INVALID_REQUEST);
        };
        return comparator.thenComparing(PatentListResponse::id, Comparator.reverseOrder());
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

    private String resolveOriginalPdfKey(PatentCreateRequest request) {
        if (request.extractJobId() == null) {
            return request.originalPdfKey();
        }

        PatentExtractJob extractJob = patentExtractJobRepository.findById(request.extractJobId())
                .orElseThrow(() -> new PatentExtractException(ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND));

        if (!extractJob.isCompleted()) {
            throw new PatentExtractException(ErrorCode.PATENT_EXTRACT_NOT_COMPLETED);
        }

        String finalObjectKey = buildFinalPdfObjectKey(request.applicationNumber());
        patentOriginalPdfStorageService.copy(extractJob.getObjectKey(), finalObjectKey);
        return finalObjectKey;
    }

    private String buildFinalPdfObjectKey(String applicationNumber) {
        return "patents/%s/patent.pdf".formatted(applicationNumber);
    }

    private PatentDetailResponse toDetailResponse(Patent patent) {
        String latestLegalStatus = patentLegalStatusRepository
                .findFirstByPatentIdOrderByChangedAtDescIdDesc(patent.getId())
                .map(legalStatus -> legalStatus.getStatus().name())
                .orElse(null);
        BigDecimal latestReportScore = reportRepository
                .findFirstByPatentIdAndStatusOrderByIdDesc(
                        patent.getId(),
                        com.skipers.skipa.domain.report.domain.ReportStatus.COMPLETED
                )
                .map(Report::getTotalScore)
                .orElse(null);
        return PatentDetailResponse.of(patent, latestLegalStatus, latestReportScore);
    }

    private Map<Long, BigDecimal> latestReportScoresByPatentId() {
        Map<Long, BigDecimal> scoresByPatentId = new HashMap<>();
        Map<Long, Long> reportIdsByPatentId = new HashMap<>();
        for (Report report : reportRepository.findAllByStatus(com.skipers.skipa.domain.report.domain.ReportStatus.COMPLETED)) {
            Long patentId = report.getPatent().getId();
            Long currentReportId = reportIdsByPatentId.get(patentId);
            if (currentReportId == null || report.getId() > currentReportId) {
                reportIdsByPatentId.put(patentId, report.getId());
                scoresByPatentId.put(patentId, report.getTotalScore());
            }
        }
        return scoresByPatentId;
    }
}
