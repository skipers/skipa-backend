package com.skipers.skipa.domain.dashboard.api;

import com.skipers.skipa.domain.department.dao.DepartmentRepository;
import com.skipers.skipa.domain.department.domain.Department;
import com.skipers.skipa.domain.patent.dao.PatentLegalStatusRepository;
import com.skipers.skipa.domain.patent.dao.PatentRepository;
import com.skipers.skipa.domain.patent.domain.Patent;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatus;
import com.skipers.skipa.domain.patent.domain.PatentLegalStatusType;
import com.skipers.skipa.domain.report.application.ReportGenerationPublisher;
import com.skipers.skipa.domain.report.application.ReportStorageService;
import com.skipers.skipa.domain.review.dao.ReviewCycleRepository;
import com.skipers.skipa.domain.review.dao.ReviewRepository;
import com.skipers.skipa.domain.review.domain.BusinessOpinion;
import com.skipers.skipa.domain.review.domain.Review;
import com.skipers.skipa.domain.review.domain.ReviewCycle;
import com.skipers.skipa.domain.review.domain.ReviewStatus;
import com.skipers.skipa.domain.user.dao.UserRepository;
import com.skipers.skipa.domain.user.domain.User;
import com.skipers.skipa.domain.user.domain.UserRole;
import com.skipers.skipa.global.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@Transactional
class DashboardControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private PatentLegalStatusRepository patentLegalStatusRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewCycleRepository reviewCycleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private ReportGenerationPublisher reportGenerationPublisher;

    @MockitoBean
    private ReportStorageService reportStorageService;

    private Department semiconductorDepartment;
    private Department batteryDepartment;
    private ReviewCycle reviewCycle;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        semiconductorDepartment = departmentRepository.save(Department.builder()
                .name("반도체 사업부")
                .build());
        batteryDepartment = departmentRepository.save(Department.builder()
                .name("배터리 사업부")
                .build());
        reviewCycle = reviewCycleRepository.save(ReviewCycle.builder()
                .year(2026)
                .quarter(2)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .build());
    }

    @Test
    void legalDashboardReturnsScreenAggregates() throws Exception {
        Patent reviewingPatent = savePatent(
                "Reviewing Patent",
                "APP-DASH-LEGAL-1",
                "반도체",
                semiconductorDepartment,
                LocalDate.now().plusMonths(1)
        );
        Patent submittedPatent = savePatent(
                "Submitted Patent",
                "APP-DASH-LEGAL-2",
                "배터리",
                batteryDepartment,
                LocalDate.now().plusMonths(4)
        );
        Patent overduePatent = savePatent(
                "Overdue Patent",
                "APP-DASH-LEGAL-3",
                "반도체",
                semiconductorDepartment,
                LocalDate.now().plusMonths(7)
        );
        Patent checkedReplyPatent = savePatent(
                "Checked Reply Patent",
                "APP-DASH-LEGAL-5",
                "소재",
                batteryDepartment,
                LocalDate.now().plusMonths(8)
        );
        savePatent(
                "Unrequested Patent",
                "APP-DASH-LEGAL-4",
                "소재",
                semiconductorDepartment,
                LocalDate.now().plusMonths(10)
        );
        reviewRepository.save(Review.builder()
                .patent(reviewingPatent)
                .department(semiconductorDepartment)
                .reviewCycle(reviewCycle)
                .dueDate(LocalDate.now().plusDays(3))
                .build());
        reviewRepository.save(Review.builder()
                .patent(submittedPatent)
                .department(batteryDepartment)
                .reviewCycle(reviewCycle)
                .opinion(BusinessOpinion.MAINTAIN)
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .checked(false)
                .build());
        reviewRepository.save(Review.builder()
                .patent(overduePatent)
                .department(semiconductorDepartment)
                .reviewCycle(reviewCycle)
                .dueDate(LocalDate.now().minusDays(1))
                .build());
        reviewRepository.save(Review.builder()
                .patent(checkedReplyPatent)
                .department(batteryDepartment)
                .reviewCycle(reviewCycle)
                .opinion(BusinessOpinion.ABANDON)
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now().minusSeconds(60))
                .checked(true)
                .build());
        String legalToken = createActiveUserToken("legal-dashboard", "legal-dashboard@example.com", UserRole.LEGAL, null);

        mockMvc.perform(get("/dashboard/legal")
                        .header("Authorization", "Bearer " + legalToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCycle.id").value(reviewCycle.getId()))
                .andExpect(jsonPath("$.data.progressRate").doesNotExist())
                .andExpect(jsonPath("$.data.kpi.requested").value(4))
                .andExpect(jsonPath("$.data.kpi.reviewing").value(1))
                .andExpect(jsonPath("$.data.kpi.decided").value(2))
                .andExpect(jsonPath("$.data.kpi.overdue").value(1))
                .andExpect(jsonPath("$.data.kpi.unread").value(1))
                .andExpect(jsonPath("$.data.kpi.unrequested").value(1))
                .andExpect(jsonPath("$.data.departments.length()").value(2))
                .andExpect(jsonPath("$.data.departments[0].reviewing").doesNotExist())
                .andExpect(jsonPath("$.data.departments[0].overdue").doesNotExist())
                .andExpect(jsonPath("$.data.recentReplies.length()").value(1))
                .andExpect(jsonPath("$.data.recentReplies[0].checked").value(false));
    }

    @Test
    void businessDashboardIsScopedToUserDepartment() throws Exception {
        Patent pendingPatent = savePatent(
                "Pending Patent",
                "APP-DASH-BIZ-1",
                "반도체",
                semiconductorDepartment,
                LocalDate.now().plusYears(2)
        );
        Patent submittedPatent = savePatent(
                "Submitted Biz Patent",
                "APP-DASH-BIZ-2",
                "반도체",
                semiconductorDepartment,
                LocalDate.now().plusMonths(6)
        );
        Patent otherDepartmentPatent = savePatent(
                "Other Department Patent",
                "APP-DASH-BIZ-3",
                "배터리",
                batteryDepartment,
                LocalDate.now().plusMonths(6)
        );
        patentLegalStatusRepository.save(PatentLegalStatus.builder()
                .patent(pendingPatent)
                .status(PatentLegalStatusType.REGISTERED)
                .changedAt(LocalDate.now())
                .build());
        Review pendingReview = reviewRepository.save(Review.builder()
                .patent(pendingPatent)
                .department(semiconductorDepartment)
                .reviewCycle(reviewCycle)
                .dueDate(LocalDate.now().plusDays(3))
                .build());
        reviewRepository.save(Review.builder()
                .patent(submittedPatent)
                .department(semiconductorDepartment)
                .reviewCycle(reviewCycle)
                .opinion(BusinessOpinion.ABANDON)
                .status(ReviewStatus.SUBMITTED)
                .submittedAt(Instant.now())
                .build());
        reviewRepository.save(Review.builder()
                .patent(otherDepartmentPatent)
                .department(batteryDepartment)
                .reviewCycle(reviewCycle)
                .dueDate(LocalDate.now().plusDays(3))
                .build());
        String businessToken = createActiveUserToken(
                "business-dashboard",
                "business-dashboard@example.com",
                UserRole.BUSINESS,
                semiconductorDepartment
        );

        mockMvc.perform(get("/dashboard/business")
                        .header("Authorization", "Bearer " + businessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCycle.id").value(reviewCycle.getId()))
                .andExpect(jsonPath("$.data.kpi.total").value(2))
                .andExpect(jsonPath("$.data.kpi.submitted").value(1))
                .andExpect(jsonPath("$.data.kpi.notSubmitted").value(1))
                .andExpect(jsonPath("$.data.dueDate").doesNotExist())
                .andExpect(jsonPath("$.data.dDay").doesNotExist())
                .andExpect(jsonPath("$.data.kpi.requested").doesNotExist())
                .andExpect(jsonPath("$.data.kpi.reviewing").doesNotExist())
                .andExpect(jsonPath("$.data.kpi.decided").doesNotExist())
                .andExpect(jsonPath("$.data.kpi.overdue").doesNotExist())
                .andExpect(jsonPath("$.data.kpi.pending").doesNotExist())
                .andExpect(jsonPath("$.data.kpi.unread").doesNotExist())
                .andExpect(jsonPath("$.data.kpi.unrequested").doesNotExist())
                .andExpect(jsonPath("$.data.patentStatus.expiringSoon").doesNotExist())
                .andExpect(jsonPath("$.data.pendingPatents.length()").value(1))
                .andExpect(jsonPath("$.data.pendingPatents[0].reviewId").value(pendingReview.getId()))
                .andExpect(jsonPath("$.data.pendingPatents[0].patentId").value(pendingPatent.getId()))
                .andExpect(jsonPath("$.data.recentSubmissions.length()").value(1))
                .andExpect(jsonPath("$.data.recentSubmissions[0].patentId").value(submittedPatent.getId()));
    }

    private Patent savePatent(
            String title,
            String applicationNumber,
            String techField,
            Department department,
            LocalDate expiryDate
    ) {
        return patentRepository.save(Patent.builder()
                .title(title)
                .applicationNumber(applicationNumber)
                .techField(techField)
                .filingCountry("KR")
                .applicationDate(LocalDate.now().minusYears(1))
                .expiryDate(expiryDate)
                .currentDepartment(department)
                .build());
    }

    private String createActiveUserToken(String loginId, String email, UserRole role, Department department) {
        User user = userRepository.save(User.createActive(
                loginId,
                loginId,
                email,
                passwordEncoder.encode("password"),
                role,
                department
        ));
        return jwtProvider.createAccessToken(user.getId(), role);
    }
}
