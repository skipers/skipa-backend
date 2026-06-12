package com.skipers.skipa.domain.patent.application;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.department.exception.DepartmentException;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
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
import com.skipers.skipa.domain.patent.exception.PatentException;
import com.skipers.skipa.domain.patentextract.dao.PatentExtractJobRepository;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJob;
import com.skipers.skipa.domain.patentextract.domain.PatentExtractJobStatus;
import com.skipers.skipa.domain.patentextract.exception.PatentExtractException;
import com.skipers.skipa.domain.portfolio.application.PortfolioInsightCacheInvalidator;
import com.skipers.skipa.domain.report.dao.ReportRepository;
import com.skipers.skipa.domain.report.domain.Report;
import com.skipers.skipa.domain.report.domain.ReportStatus;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PatentServiceTest {

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PatentLegalStatusRepository patentLegalStatusRepository;

    @Mock
    private PatentAnnuityRepository patentAnnuityRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private BusinessPatentAccessValidator businessPatentAccessValidator;

    @Mock
    private PatentExtractJobRepository patentExtractJobRepository;

    @Mock
    private PatentOriginalPdfStorageService patentOriginalPdfStorageService;

    @Mock
    private PortfolioInsightCacheInvalidator portfolioInsightCacheInvalidator;

    @InjectMocks
    private PatentService patentService;

    @BeforeEach
    void setUp() {
        lenient().when(patentLegalStatusRepository.findFirstByPatentIdOrderByChangedAtDescIdDesc(any()))
                .thenReturn(Optional.empty());
        lenient().when(reportRepository.findFirstByPatentIdAndStatusInOrderByIdDesc(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(reportRepository.findAllByStatusIn(any()))
                .thenReturn(List.of());
    }

    @Test
    void createPreservesTitleAndApplicationNumber() {
        when(patentRepository.save(any(Patent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatentDetailResponse response = patentService.create(legalUser(), createRequest("  Patent Title  ", " 10-1234 "));

        verify(patentRepository).existsByApplicationNumber(" 10-1234 ");
        verify(patentRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getTitle().equals("  Patent Title  ")
                        && saved.getApplicationNumber().equals(" 10-1234 ")
                        && saved.getInitialDepartment().equals(" Initial Department ")
        ));
        assertThat(response.title()).isEqualTo("  Patent Title  ");
        assertThat(response.initialDepartment()).isEqualTo(" Initial Department ");
        assertThat(response.ipcCodes()).containsExactly("IPC");
        assertThat(response.cpcCodes()).containsExactly("CPC");
        assertThat(response.relatedProducts()).containsExactly("Product");
        assertThat(response.keywords()).containsExactly("Keyword");
    }

    @Test
    void createRejectsDuplicateApplicationNumber() {
        when(patentRepository.existsByApplicationNumber("APP-1")).thenReturn(true);

        assertPatentError(
                () -> patentService.create(legalUser(), createRequest("Patent", "APP-1")),
                ErrorCode.DUPLICATE_APPLICATION_NUMBER
        );

        verify(patentRepository, never()).save(any());
    }

    @Test
    void createWithoutExtractJobPreservesOriginalPdfKey() {
        when(patentRepository.save(any(Patent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatentDetailResponse response = patentService.create(legalUser(), createRequest("Patent", "APP-1"));

        assertThat(response.originalPdfKey()).isEqualTo("pdf-key");
        assertThat(response.approvalStatus()).isEqualTo("APPROVED");
        verify(patentExtractJobRepository, never()).findById(any());
        verify(patentOriginalPdfStorageService, never()).copy(any(), any());
    }

    @Test
    void createByBusinessUserCreatesPendingApprovalPatent() {
        Department department = department("Business", 1L);
        when(patentRepository.save(any(Patent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatentDetailResponse response = patentService.create(businessUser(department), createRequest("Patent", "APP-1"));

        assertThat(response.approvalStatus()).isEqualTo("PENDING_APPROVAL");
        assertThat(response.currentDepartmentId()).isEqualTo(1L);
        verify(patentRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getApprovalStatus().name().equals("PENDING_APPROVAL")
                        && saved.getCurrentDepartment().getId().equals(1L)
        ));
    }

    @Test
    void createWithExtractJobCopiesTemporaryPdfAndStoresFinalPdfKey() {
        PatentExtractJob extractJob = completedExtractJob(7L, "tmp/patent-extract-jobs/7/original.pdf");
        when(patentExtractJobRepository.findById(7L)).thenReturn(Optional.of(extractJob));
        when(patentRepository.save(any(Patent.class))).thenAnswer(invocation -> {
            Patent patent = invocation.getArgument(0);
            ReflectionTestUtils.setField(patent, "id", 1L);
            return patent;
        });

        PatentDetailResponse response = patentService.create(legalUser(), createRequestWithExtractJob("Patent", "10-2026-0000000", 7L));

        assertThat(response.originalPdfKey()).isEqualTo("patents/1/original.pdf");
        assertThat(response.parsedJsonKey()).isEqualTo("patents/1/parsed.json");
        verify(patentOriginalPdfStorageService).copy(
                "tmp/patent-extract-jobs/7/original.pdf",
                "patents/1/original.pdf"
        );
        verify(patentOriginalPdfStorageService).saveJson(
                org.mockito.ArgumentMatchers.eq("patents/1/parsed.json"),
                org.mockito.ArgumentMatchers.same(extractJob.getResultJson())
        );
    }

    @Test
    void createRejectsMissingExtractJob() {
        when(patentExtractJobRepository.findById(7L)).thenReturn(Optional.empty());

        assertPatentExtractError(
                () -> patentService.create(legalUser(), createRequestWithExtractJob("Patent", "APP-1", 7L)),
                ErrorCode.PATENT_EXTRACT_JOB_NOT_FOUND
        );

        verify(patentRepository, never()).save(any());
        verify(patentOriginalPdfStorageService, never()).copy(any(), any());
    }

    @Test
    void createRejectsExtractJobThatIsNotCompleted() {
        PatentExtractJob extractJob = PatentExtractJob.builder()
                .objectKey("tmp/patent-extract-jobs/7/original.pdf")
                .status(PatentExtractJobStatus.ANALYZING)
                .build();
        ReflectionTestUtils.setField(extractJob, "id", 7L);
        when(patentExtractJobRepository.findById(7L)).thenReturn(Optional.of(extractJob));

        assertPatentExtractError(
                () -> patentService.create(legalUser(), createRequestWithExtractJob("Patent", "APP-1", 7L)),
                ErrorCode.PATENT_EXTRACT_NOT_COMPLETED
        );

        verify(patentRepository, never()).save(any());
        verify(patentOriginalPdfStorageService, never()).copy(any(), any());
    }

    @Test
    void getRejectsMissingPatent() {
        when(patentRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentError(() -> patentService.get(1L), ErrorCode.PATENT_NOT_FOUND);
    }

    @Test
    void getReturnsLatestLegalStatusInDetailResponse() {
        Patent patent = patent(
                1L,
                "Patent",
                "APP-DETAIL",
                "반도체",
                "KR",
                LocalDate.now().plusYears(1),
                department("통신", 1L)
        );
        PatentLegalStatus latestStatus = legalStatus(
                20L,
                patent,
                PatentLegalStatusType.REGISTERED,
                LocalDate.now()
        );
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));
        when(patentLegalStatusRepository.findFirstByPatentIdOrderByChangedAtDescIdDesc(1L))
                .thenReturn(Optional.of(latestStatus));
        when(reportRepository.findFirstByPatentIdAndStatusInOrderByIdDesc(
                1L,
                List.of(ReportStatus.REPORT_COMPLETED, ReportStatus.EMBEDDING_COMPLETED)
        ))
                .thenReturn(Optional.of(report(1L, patent, new BigDecimal("82.50"))));

        PatentDetailResponse response = patentService.get(1L);

        assertThat(response.latestLegalStatus()).isEqualTo("REGISTERED");
        assertThat(response.latestReportScore()).isEqualByComparingTo("82.50");
        assertThat(response.currentDepartmentName()).isEqualTo("통신");
    }

    @Test
    void getAllWithoutKeywordUsesPagedFindAll() {
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
        Pageable pageable = PageRequest.of(0, 20);
        Pageable sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "applicationNumber")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        when(patentRepository.findAll(sortedPageable)).thenReturn(new PageImpl<>(List.of(patent), sortedPageable, 1));

        Page<?> result = patentService.getAll(legalUser(), "  ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(patentRepository).findAll(sortedPageable);
    }

    @Test
    void getAllWithKeywordSearchesPatentFields() {
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
        Pageable pageable = PageRequest.of(0, 20);
        Pageable sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "applicationNumber")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        when(patentRepository.searchByKeyword("patent", sortedPageable))
                .thenReturn(new PageImpl<>(List.of(patent), sortedPageable, 1));

        Page<?> result = patentService.getAll(legalUser(), " patent ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(patentRepository).searchByKeyword("patent", sortedPageable);
    }

    @Test
    void getAllForBusinessUserSearchesAllPatents() {
        Department department = Department.builder().name("Telecom").build();
        ReflectionTestUtils.setField(department, "id", 1L);
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
        Pageable pageable = PageRequest.of(0, 20);
        Pageable sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "applicationNumber")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        when(patentRepository.searchByKeyword("patent", sortedPageable))
                .thenReturn(new PageImpl<>(List.of(patent), sortedPageable, 1));

        Page<?> result = patentService.getAll(businessUser(department), " patent ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(patentRepository).searchByKeyword("patent", sortedPageable);
        verify(patentRepository, never()).searchByCurrentDepartmentIdAndKeyword(1L, "patent", sortedPageable);
    }

    @Test
    void getAllAllowsBusinessUserWithoutDepartment() {
        Pageable pageable = PageRequest.of(0, 20);
        Pageable sortedPageable = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.ASC, "applicationNumber")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        when(patentRepository.findAll(sortedPageable)).thenReturn(new PageImpl<>(List.of(), sortedPageable, 0));

        Page<?> result = patentService.getAll(businessUser(null), null, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(patentRepository).findAll(sortedPageable);
    }

    @Test
    void getAssignedFiltersBusinessUserDepartment() {
        Department department = department("Telecom", 1L);
        Patent assignedPatent = patent(
                1L,
                "Assigned Patent",
                "APP-ASSIGNED",
                "반도체",
                "KR",
                LocalDate.now().plusYears(1),
                department
        );
        when(patentRepository.findAll()).thenReturn(List.of(assignedPatent));
        when(patentLegalStatusRepository.findAll()).thenReturn(List.of());

        Page<?> result = patentService.getAssigned(
                businessUser(department),
                null,
                "title,asc",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0))
                .extracting("id", "currentDepartmentId")
                .containsExactly(1L, 1L);
    }

    @Test
    void getAssignedRejectsBusinessUserWithoutDepartment() {
        assertPatentError(
                () -> patentService.getAssigned(businessUser(null), null, null, PageRequest.of(0, 20)),
                ErrorCode.FORBIDDEN
        );
    }

    @Test
    void getSummaryCountsMaintainAndAbandonStatusesForLegalUser() {
        Patent appliedPatent = patent(1L, "Applied Patent", "APP-SUM-1", null, null, null, null);
        Patent registeredPatent = patent(2L, "Registered Patent", "APP-SUM-2", null, null, null, null);
        Patent expiredPatent = patent(3L, "Expired Patent", "APP-SUM-3", null, null, null, null);
        Patent noStatusPatent = patent(4L, "No Status Patent", "APP-SUM-4", null, null, null, null);
        when(patentRepository.findAll()).thenReturn(List.of(appliedPatent, registeredPatent, expiredPatent, noStatusPatent));
        when(patentLegalStatusRepository.findAll()).thenReturn(List.of(
                legalStatus(10L, appliedPatent, PatentLegalStatusType.APPLIED, LocalDate.now()),
                legalStatus(20L, registeredPatent, PatentLegalStatusType.PUBLISHED, LocalDate.now().minusDays(1)),
                legalStatus(21L, registeredPatent, PatentLegalStatusType.REGISTERED, LocalDate.now()),
                legalStatus(30L, expiredPatent, PatentLegalStatusType.EXPIRED, LocalDate.now())
        ));

        var response = patentService.getSummary(legalUser());

        assertThat(response.active()).isEqualTo(2);
        assertThat(response.inactive()).isEqualTo(2);
    }

    @Test
    void getSummaryCountsOnlyBusinessUsersDepartmentPatents() {
        Department department = department("통신", 1L);
        Patent maintainPatent = patent(1L, "Maintain Patent", "APP-SUM-BIZ-1", null, null, null, department);
        Patent abandonPatent = patent(2L, "Abandon Patent", "APP-SUM-BIZ-2", null, null, null, department);
        when(patentRepository.findByCurrentDepartmentId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(maintainPatent, abandonPatent)));
        when(patentLegalStatusRepository.findAll()).thenReturn(List.of(
                legalStatus(10L, maintainPatent, PatentLegalStatusType.REGISTERED, LocalDate.now()),
                legalStatus(20L, abandonPatent, PatentLegalStatusType.WITHDRAWN, LocalDate.now())
        ));

        var response = patentService.getSummary(businessUser(department));

        assertThat(response.active()).isEqualTo(1);
        assertThat(response.inactive()).isEqualTo(1);
        verify(patentRepository).findByCurrentDepartmentId(1L, Pageable.unpaged());
        verify(patentRepository, never()).findAll();
    }

    @Test
    void getSummaryRejectsBusinessUserWithoutDepartment() {
        assertPatentError(
                () -> patentService.getSummary(businessUser(null)),
                ErrorCode.FORBIDDEN
        );
    }

    @Test
    void getAllAppliesLegalScreenFiltersAndReturnsExpandedFields() {
        Department telecom = department("통신", 1L);
        Department battery = department("배터리", 2L);
        Patent maintainPatent = patent(
                1L,
                "Maintain Patent",
                "APP-LIST-1",
                "반도체",
                "KR",
                LocalDate.now().plusDays(10),
                telecom
        );
        Patent abandonPatent = patent(
                2L,
                "Abandon Patent",
                "APP-LIST-2",
                "배터리",
                "US",
                LocalDate.now().plusDays(20),
                battery
        );
        when(patentRepository.findAll()).thenReturn(List.of(abandonPatent, maintainPatent));
        when(patentLegalStatusRepository.findAll()).thenReturn(List.of(
                legalStatus(100L, maintainPatent, PatentLegalStatusType.REGISTERED, LocalDate.now()),
                legalStatus(200L, abandonPatent, PatentLegalStatusType.EXPIRED, LocalDate.now())
        ));
        Page<?> result = patentService.getAll(
                legalUser(),
                null,
                1L,
                List.of("REGISTERED"),
                "KR",
                "expiryDate",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        Object item = result.getContent().get(0);
        assertThat(item)
                .extracting("id", "latestLegalStatus", "techField", "currentDepartmentId", "currentDepartmentName",
                        "filingCountry")
                .containsExactly(1L, "REGISTERED", "반도체", 1L, "통신", "KR");
    }

    @Test
    void getAllSortsByPatentFieldsWithDirection() {
        Patent alphaFirstPatent = patent(
                1L,
                "Alpha Patent",
                "APP-SORT-B",
                "반도체",
                "KR",
                LocalDate.now().plusDays(10),
                null
        );
        Patent alphaSecondPatent = patent(
                2L,
                "Alpha Patent",
                "APP-SORT-C",
                "반도체",
                "KR",
                LocalDate.now().plusDays(20),
                null
        );
        Patent betaPatent = patent(
                3L,
                "Beta Patent",
                "APP-SORT-A",
                "반도체",
                "KR",
                LocalDate.now().plusDays(30),
                null
        );
        when(patentRepository.findAll()).thenReturn(List.of(betaPatent, alphaFirstPatent, alphaSecondPatent));
        when(patentLegalStatusRepository.findAll()).thenReturn(List.of());

        Page<?> result = patentService.getAll(
                legalUser(),
                null,
                null,
                null,
                null,
                "title,desc",
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting("id")
                .containsExactly(3L, 2L, 1L);
    }

    @Test
    void getAllExcludesPendingApprovalPatentsByDefault() {
        Patent approvedPatent = patent(
                1L,
                "Approved Patent",
                "APP-APPROVED",
                null,
                null,
                null,
                null
        );
        Patent pendingPatent = patent(
                2L,
                "Pending Patent",
                "APP-PENDING",
                null,
                null,
                null,
                null,
                PatentApprovalStatus.PENDING_APPROVAL
        );
        when(patentRepository.findAll()).thenReturn(List.of(approvedPatent, pendingPatent));
        when(patentLegalStatusRepository.findAll()).thenReturn(List.of());

        Page<?> result = patentService.getAll(
                legalUser(),
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting("id")
                .containsExactly(1L);
    }

    @Test
    void getPendingApprovalsReturnsPendingApprovalPatents() {
        Patent approvedPatent = patent(
                1L,
                "Approved Patent",
                "APP-APPROVED",
                null,
                null,
                null,
                null
        );
        Patent pendingPatent = patent(
                2L,
                "Pending Patent",
                "APP-PENDING",
                null,
                null,
                null,
                null,
                PatentApprovalStatus.PENDING_APPROVAL
        );
        when(patentRepository.findAll()).thenReturn(List.of(approvedPatent, pendingPatent));
        when(patentLegalStatusRepository.findAll()).thenReturn(List.of());

        Page<?> result = patentService.getPendingApprovals(
                legalUser(),
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent())
                .extracting("id", "approvalStatus")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(2L, "PENDING_APPROVAL"));
    }

    @Test
    void getAllFiltersUnassignedPatentsByDepartmentSentinel() {
        Patent assignedPatent = patent(
                1L,
                "Assigned Patent",
                "APP-LIST-ASSIGNED",
                "반도체",
                "KR",
                null,
                department("통신", 1L)
        );
        Patent unassignedPatent = patent(
                2L,
                "Unassigned Patent",
                "APP-LIST-UNASSIGNED",
                null,
                null,
                null,
                null
        );
        when(patentRepository.findAll()).thenReturn(List.of(assignedPatent, unassignedPatent));
        when(patentLegalStatusRepository.findAll()).thenReturn(List.of());

        Page<?> result = patentService.getAll(
                legalUser(),
                null,
                -1L,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0))
                .extracting("id", "currentDepartmentId")
                .containsExactly(2L, null);
    }

    @Test
    void updateReplacesPatentFieldsAndPreservesApplicationNumber() {
        Patent patent = Patent.builder()
                .title("Old Title")
                .applicationNumber("OLD-APP")
                .build();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));

        PatentDetailResponse response = patentService.update(1L, updateRequest("  Updated Title  ", " NEW-APP "));

        assertThat(response.title()).isEqualTo("  Updated Title  ");
        assertThat(response.applicationNumber()).isEqualTo(" NEW-APP ");
        assertThat(response.registrationNumber()).isEqualTo("REG-1");
        assertThat(response.applicationDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.applicant()).isEqualTo("Applicant");
        assertThat(response.examinationClaimCount()).isEqualTo(5);
        assertThat(response.ipcCodes()).containsExactly("IPC");
        assertThat(response.cpcCodes()).containsExactly("CPC");
        assertThat(response.initialDepartment()).isEqualTo("Initial Department");
        assertThat(response.relatedProducts()).containsExactly("Product");
        assertThat(response.keywords()).containsExactly("Keyword");
        assertThat(response.summary()).isEqualTo("Summary");
        verify(patentRepository).existsByApplicationNumber(" NEW-APP ");
    }

    @Test
    void updateRejectsDuplicateChangedApplicationNumber() {
        Patent patent = Patent.builder()
                .title("Old Title")
                .applicationNumber("OLD-APP")
                .build();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));
        when(patentRepository.existsByApplicationNumber("NEW-APP")).thenReturn(true);

        assertPatentError(
                () -> patentService.update(1L, updateRequest("Updated Title", "NEW-APP")),
                ErrorCode.DUPLICATE_APPLICATION_NUMBER
        );

        assertThat(patent.getTitle()).isEqualTo("Old Title");
        assertThat(patent.getApplicationNumber()).isEqualTo("OLD-APP");
    }

    @Test
    void updateWithUnchangedApplicationNumberDoesNotCheckDuplicate() {
        Patent patent = Patent.builder()
                .title("Old Title")
                .applicationNumber("APP-1")
                .build();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));

        PatentDetailResponse response = patentService.update(1L, updateRequest("Updated Title", "APP-1"));

        assertThat(response.title()).isEqualTo("Updated Title");
        verify(patentRepository, never()).existsByApplicationNumber(any());
    }

    @Test
    void approveChangesPendingApprovalPatentToApproved() {
        Patent patent = patent(
                1L,
                "Pending Patent",
                "APP-PENDING",
                null,
                null,
                null,
                null,
                PatentApprovalStatus.PENDING_APPROVAL
        );
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));

        PatentDetailResponse response = patentService.approve(1L);

        assertThat(response.approvalStatus()).isEqualTo("APPROVED");
        assertThat(patent.getApprovalStatus()).isEqualTo(PatentApprovalStatus.APPROVED);
    }

    @Test
    void approveRejectsAlreadyApprovedPatent() {
        Patent patent = patent(
                1L,
                "Approved Patent",
                "APP-APPROVED",
                null,
                null,
                null,
                null
        );
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));

        assertPatentError(() -> patentService.approve(1L), ErrorCode.PATENT_APPROVAL_NOT_PENDING);
    }

    @Test
    void updateRejectsMissingPatent() {
        when(patentRepository.findById(1L)).thenReturn(Optional.empty());

        assertPatentError(() -> patentService.update(1L, null), ErrorCode.PATENT_NOT_FOUND);
    }

    @Test
    void changeDepartmentRejectsInactiveDepartment() {
        Patent patent = Patent.builder().title("Patent").applicationNumber("APP-1").build();
        Department department = Department.builder().name("Telecom").build();
        department.deactivate();
        when(patentRepository.findById(1L)).thenReturn(Optional.of(patent));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));

        assertThatThrownBy(() -> patentService.changeDepartment(1L, new PatentDepartmentChangeRequest(10L)))
                .isInstanceOfSatisfying(DepartmentException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_INACTIVE));

        assertThat(patent.getCurrentDepartment()).isNull();
    }

    @Test
    void deleteRemovesExistingPatent() {
        when(patentRepository.existsById(1L)).thenReturn(true);

        patentService.delete(1L);

        InOrder deletionOrder = inOrder(patentLegalStatusRepository, patentAnnuityRepository, reviewRepository, reportRepository, patentRepository);
        deletionOrder.verify(patentLegalStatusRepository).deleteAllByPatentId(1L);
        deletionOrder.verify(patentAnnuityRepository).deleteAllByPatentId(1L);
        deletionOrder.verify(reviewRepository).deleteAllByPatentId(1L);
        deletionOrder.verify(reportRepository).deleteAllByPatentId(1L);
        deletionOrder.verify(patentRepository).deleteById(1L);
    }

    @Test
    void deleteRejectsMissingPatent() {
        when(patentRepository.existsById(1L)).thenReturn(false);

        assertPatentError(() -> patentService.delete(1L), ErrorCode.PATENT_NOT_FOUND);

        verify(patentLegalStatusRepository, never()).deleteAllByPatentId(1L);
        verify(patentAnnuityRepository, never()).deleteAllByPatentId(1L);
        verify(reviewRepository, never()).deleteAllByPatentId(1L);
        verify(reportRepository, never()).deleteAllByPatentId(1L);
        verify(patentRepository, never()).deleteById(1L);
    }

    private Department department(String name, Long id) {
        Department department = Department.builder().name(name).build();
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }

    private Patent patent(
            Long id,
            String title,
            String applicationNumber,
            String techField,
            String filingCountry,
            LocalDate expiryDate,
            Department department
    ) {
        return patent(id, title, applicationNumber, techField, filingCountry, expiryDate, department, PatentApprovalStatus.APPROVED);
    }

    private Patent patent(
            Long id,
            String title,
            String applicationNumber,
            String techField,
            String filingCountry,
            LocalDate expiryDate,
            Department department,
            PatentApprovalStatus approvalStatus
    ) {
        Patent patent = Patent.builder()
                .title(title)
                .applicationNumber(applicationNumber)
                .techField(techField)
                .filingCountry(filingCountry)
                .expiryDate(expiryDate)
                .currentDepartment(department)
                .approvalStatus(approvalStatus)
                .build();
        ReflectionTestUtils.setField(patent, "id", id);
        return patent;
    }

    private PatentLegalStatus legalStatus(
            Long id,
            Patent patent,
            PatentLegalStatusType status,
            LocalDate changedAt
    ) {
        PatentLegalStatus legalStatus = PatentLegalStatus.builder()
                .patent(patent)
                .status(status)
                .changedAt(changedAt)
                .build();
        ReflectionTestUtils.setField(legalStatus, "id", id);
        return legalStatus;
    }

    private Report report(Long id, Patent patent, BigDecimal totalScore) {
        Report report = Report.builder()
                .patent(patent)
                .status(ReportStatus.REPORT_COMPLETED)
                .totalScore(totalScore)
                .build();
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    private PatentCreateRequest createRequest(String title, String applicationNumber) {
        return new PatentCreateRequest(
                title,
                applicationNumber,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("IPC"),
                List.of("CPC"),
                null,
                null,
                null,
                null,
                null,
                "pdf-key",
                null,
                null,
                null,
                null,
                List.of("Product"),
                null,
                null,
                null,
                " Initial Department ",
                List.of("Keyword"),
                null
        );
    }

    private PatentCreateRequest createRequestWithExtractJob(String title, String applicationNumber, Long extractJobId) {
        return new PatentCreateRequest(
                title,
                applicationNumber,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "ignored-pdf-key",
                extractJobId,
                null,
                null,
                null,
                List.of("Product"),
                null,
                null,
                null,
                " Initial Department ",
                List.of("Keyword"),
                null
        );
    }

    private PatentExtractJob completedExtractJob(Long extractJobId, String objectKey) {
        PatentExtractJob extractJob = PatentExtractJob.builder()
                .objectKey(objectKey)
                .status(PatentExtractJobStatus.COMPLETED)
                .build();
        ReflectionTestUtils.setField(extractJob, "id", extractJobId);
        return extractJob;
    }

    private User legalUser() {
        return User.createActive("legal", "Legal", "legal@example.com", "password", UserRole.LEGAL, null);
    }

    private User businessUser(Department department) {
        return User.createActive("business", "Business", "business@example.com", "password", UserRole.BUSINESS, department);
    }

    private PatentUpdateRequest updateRequest(String title, String applicationNumber) {
        return new PatentUpdateRequest(
                title,
                applicationNumber,
                "REG-1",
                "PUB-1",
                "ANN-1",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 3),
                LocalDate.of(2026, 1, 4),
                List.of("IPC"),
                List.of("CPC"),
                "Applicant",
                "Inventor",
                LocalDate.of(2046, 1, 1),
                3,
                5,
                "pdf-key",
                "management",
                "business",
                "tech",
                List.of("Product"),
                "KR",
                true,
                "Joint Applicant",
                "Initial Department",
                List.of("Keyword"),
                "Summary"
        );
    }

    private void assertPatentError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(PatentException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private void assertPatentExtractError(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(PatentExtractException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
